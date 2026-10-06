<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, ElTag, type FormInstance, type FormRules } from 'element-plus'
import { api } from '../api'
import QuestionFields from '../components/QuestionFields.vue'
import { emptyQuestion, decodeQuestion, questionPayload, questionTypes, difficulties } from '../utils/questionForm'
import { formatTime, parseUtc, timeZoneLabel } from '../utils/dateTime'
import { useAuthStore } from '../stores/auth'
import { passwordValidationMessage, realNameValidationMessage, usernameValidationMessage } from '../utils/formValidation'

type Row = Record<string, any>
const auth=useAuthStore()
const role=auth.user?.role || 'STUDENT'
const roleNames:Record<string,string>={ADMIN:'管理员',TEACHER:'教师',STUDENT:'学生'}
const menus:Record<string,{key:string,label:string}[]>={
  ADMIN:[{key:'overview',label:'工作台'},{key:'users',label:'用户与教师'},{key:'courses',label:'课程管理'},{key:'classes',label:'班级管理'},{key:'classrooms',label:'教室管理'},{key:'schedules',label:'课次排程'},{key:'orders',label:'报名订单'},{key:'auditLogs',label:'操作日志'}],
  TEACHER:[{key:'overview',label:'工作台'},{key:'classes',label:'我的班级'},{key:'students',label:'班级学生'},{key:'schedules',label:'我的课表'},{key:'attendance',label:'考勤登记'},{key:'assignments',label:'作业管理'},{key:'submissions',label:'作业批改'},{key:'questions',label:'题库管理'},{key:'exams',label:'考试管理'},{key:'results',label:'考试成绩'}],
  STUDENT:[{key:'overview',label:'学习概览'},{key:'courses',label:'课程与报名'},{key:'myClasses',label:'我的班级'},{key:'orders',label:'我的订单'},{key:'schedules',label:'我的课表'},{key:'attendance',label:'我的考勤'},{key:'assignments',label:'我的作业'},{key:'exams',label:'我的考试'},{key:'notifications',label:'站内通知'}],
}
const activeView=ref('overview')
const experienceGuides:Record<string,{intro:string,steps:{view:string,title:string,description:string}[]}>={
  ADMIN:{intro:'从课程目录到教学班，再安排课次；已准备好教学中和报名中的示例班级。',steps:[{view:'courses',title:'查看课程',description:'课程介绍、价格与上架状态'},{view:'classes',title:'管理教学班',description:'查看所属课程、任课教师和报名人数'},{view:'schedules',title:'安排课次',description:'选择班级和教室，体验排课'},{view:'orders',title:'查看报名订单',description:'查看学生报名与模拟支付记录'}]},
  TEACHER:{intro:'已有班级、学生和题库，可直接登记考勤、批改作业和查看考试成绩。',steps:[{view:'attendance',title:'登记考勤',description:'选课次 → 全员出勤 → 保存考勤'},{view:'submissions',title:'批改作业',description:'已有待批作业，可评分并填写评语'},{view:'results',title:'查看考试成绩',description:'切换考试，也可批改简答题'},{view:'exams',title:'体验组卷',description:'选择草稿考试，从现有题库抽题并发布'}]},
  STUDENT:{intro:'已加入 Java 实战班，可提交作业、参加在线测验；也可报名新班体验模拟支付。',steps:[{view:'assignments',title:'提交作业',description:'有待提交练习，也有已批改作业'},{view:'exams',title:'参加考试',description:'选择在线体验测验开始答题，或查看历史成绩'},{view:'courses',title:'报名新课程',description:'查看开放班级 → 报名 → 模拟支付'},{view:'schedules',title:'查看学习安排',description:'查看课表、任课教师和上课时间'}]},
}

const loading=ref(false)
const rows=ref<Row[]>([])
const tablePage=ref(1),tablePageSize=ref(20),tableTotal=ref(0)
const courses=ref<Row[]>([]),classes=ref<Row[]>([]),rooms=ref<Row[]>([]),teachers=ref<Row[]>([]),schedules=ref<Row[]>([]),members=ref<Row[]>([]),openClasses=ref<Row[]>([])
const teacherAssignments=ref<Row[]>([])
const teacherExams=ref<Row[]>([])
const selectedClassId=ref<number|undefined>(),selectedScheduleId=ref<number|undefined>(),selectedAssignmentId=ref<number|undefined>(),selectedExamId=ref<number|undefined>(),selectedCourseId=ref<number|undefined>()
const selectedCourse=ref<Row|null>(null)
const courseEnrollments=ref<Row[]>([])
function courseEnrollment(courseId:number){return courseEnrollments.value.find(x=>Number(x.courseId)===Number(courseId)&&x.status==='ENROLLED')||courseEnrollments.value.find(x=>Number(x.courseId)===Number(courseId)&&x.status==='PENDING')}
function canPayOrder(order:Row){const pending=courseEnrollments.value.find(x=>Number(x.orderId)===Number(order.id)&&x.status==='PENDING');return !!pending&&!courseEnrollments.value.some(x=>Number(x.courseId)===Number(pending.courseId)&&x.status==='ENROLLED')}
const attendanceRoster=ref<Row[]>([])
const assignmentRoster=ref<Row[]>([])
const adminDashboard=ref<Row|null>(null),teacherDashboard=ref<Row|null>(null),studentDashboard=ref<Row|null>(null),classAnalytics=ref<Row|null>(null)
const examStart=ref<Row|null>(null)
const examDialogVisible=ref(false)
const paymentDialogVisible=ref(false),paymentLoading=ref(false)
const activePaymentOrder=ref<Row|null>(null)
const paymentContext=ref({courseName:'',className:''})
const examResultVisible=ref(false)
const examResult=ref<Row|null>(null)
const submissionDialogVisible=ref(false)
const activeSubmissionAssignment=ref<Row|null>(null)
const answers=reactive<Record<number,any>>({})
const answerSaveState=ref('未开始')
let answerSaveTimer:ReturnType<typeof setTimeout>|undefined
let answerSaveQueue:Promise<void>=Promise.resolve()
const notificationUnread=ref(0)
const classEditVisible=ref(false),roomEditVisible=ref(false),assignmentEditVisible=ref(false),questionEditVisible=ref(false)
const submissionGradeVisible=ref(false),examGradeVisible=ref(false)
const gradingSubmission=ref<Row|null>(null),gradingExamAnswer=ref<Row|null>(null)
const scheduleEditId=ref<number|undefined>(),pendingExamAnswers=ref<Row[]>([])
const classEditForm=reactive({id:0,courseId:0,teacherId:0,name:'',capacity:1,startDate:'',endDate:''})
const roomEditForm=reactive({id:0,name:'',capacity:1})
const assignmentEditForm=reactive({id:0,classId:0,title:'',content:'',deadline:''})
const questionEditForm=reactive(emptyQuestion())
const courseForm=reactive({name:'',description:'',price:0})
const teacherForm=reactive({username:'',password:'',realName:''})
const teacherFormRef=ref<FormInstance>()
const teacherRules:FormRules={
  username:[{validator:(_rule,value,callback)=>{const message=usernameValidationMessage(String(value??''));callback(message?new Error(message):undefined)},trigger:'blur'}],
  realName:[{validator:(_rule,value,callback)=>{const message=realNameValidationMessage(String(value??''));callback(message?new Error(message):undefined)},trigger:'blur'}],
  password:[{validator:(_rule,value,callback)=>{const message=passwordValidationMessage(String(value??''));callback(message?new Error(message):undefined)},trigger:'blur'}],
}
const classForm=reactive({courseId:undefined as number|undefined,teacherId:undefined as number|undefined,name:'',capacity:30,startDate:'',endDate:''})
const roomForm=reactive({name:'',capacity:30})
const scheduleForm=reactive({classId:undefined as number|undefined,classroomId:undefined as number|undefined,startTime:'',endTime:''})
const assignmentForm=reactive({classId:undefined as number|undefined,title:'',content:'',deadline:''})
const submissionForm=reactive({assignmentId:undefined as number|undefined,content:''})
const gradeForm=reactive({submissionId:undefined as number|undefined,score:90,feedback:''})
const questionForm=reactive(emptyQuestion())
const examForm=reactive({classId:undefined as number|undefined,title:'阶段测验',startTime:'',endTime:'',durationMinutes:60})
const ruleForm=reactive({type:'SINGLE_CHOICE',difficulty:'EASY',count:5,score:10})
const examAnswerForm=reactive({answerId:undefined as number|undefined,score:0,feedback:''})
const activeMenu=computed(()=>menus[role].find(m=>m.key===activeView.value)?.label||'工作台')
const displayName=computed(()=>auth.user?.realName||auth.user?.username||'用户')
const showPagination=computed(()=>tableTotal.value>0&&((role==='ADMIN'&&['users','courses','classes','orders','auditLogs'].includes(activeView.value))||(role==='STUDENT'&&['courses','orders'].includes(activeView.value))))
const columns=computed(()=>{
  const configs:Record<string,[string,string][]>={
    users:[['id','ID'],['username','用户名'],['realName','姓名'],['role','角色'],['status','状态']],auditLogs:[['id','记录 ID'],['actorId','操作人 ID'],['actorRole','角色'],['httpMethod','方法'],['requestPath','接口'],['responseStatus','HTTP 状态'],['outcome','结果'],['requestId','请求 ID'],['createdAt','时间']],courses:[['id','ID'],['name','课程名称'],['price','价格'],['status','状态'],['createdAt','创建时间']],
    classes:[['id','ID'],['name','班级名称'],['courseName','所属课程'],['teacherName','主讲教师'],['capacity','容量'],['reservedCount','预占'],['enrolledCount','已报名'],['startDate','开课日期'],['status','状态']],
    classrooms:[['id','ID'],['name','教室'],['capacity','容量'],['status','状态']],schedules:[['id','课次 ID'],['classId','班级'],['teacherName','任课教师'],['classroomId','教室'],['startTime','开始'],['endTime','结束'],['status','状态']],
    orders:[['id','订单 ID'],['orderNo','订单号'],['studentId','学生'],['classId','班级'],['amount','金额'],['status','状态'],['expireAt','截止时间']],
    students:[['studentId','学生 ID'],['username','用户名'],['realName','姓名'],['enrolledAt','入班时间']],attendance:[['studentId','学生 ID'],['realName','学生'],['status','考勤'],['checkedAt','登记时间']],
    assignments:[['id','作业 ID'],['title','标题'],['classId','班级'],['deadline','截止时间'],['status','状态']],submissions:[['studentId','学生'],['studentName','姓名'],['status','提交状态'],['content','提交内容'],['score','分数'],['feedback','评语'],['submittedAt','提交时间']],
    questions:[['id','题目 ID'],['courseId','课程'],['type','题型'],['content','题目'],['difficulty','难度'],['status','状态']],exams:[['id','考试 ID'],['title','考试'],['classId','班级'],['startTime','开始时间'],['endTime','结束时间'],['durationMinutes','时长'],['status','状态'],['questionCount','题数']],
    results:[['attemptId','答卷 ID'],['studentId','学生'],['studentName','姓名'],['status','状态'],['score','成绩'],['submittedAt','提交时间']],coursesStudent:[['id','课程 ID'],['name','课程'],['description','课程说明'],['price','价格'],['status','状态']],
    myClasses:[['classId','班级 ID'],['courseName','课程'],['className','班级'],['teacherName','任课教师'],['startDate','开始日期'],['endDate','结束日期']],notifications:[['id','通知 ID'],['title','标题'],['content','内容'],['createdAt','收到时间'],['readAt','已读时间']],
    studentAssignments:[['id','作业 ID'],['className','班级'],['title','标题'],['deadline','截止时间'],['submissionStatus','提交状态'],['score','分数'],['feedback','评语']],
    studentExams:[['id','考试 ID'],['className','班级'],['title','考试'],['startTime','开始'],['endTime','结束'],['durationMinutes','时长'],['status','考试状态'],['attemptStatus','答卷状态']],studentOrders:[['id','订单 ID'],['orderNo','订单号'],['classId','班级'],['amount','金额'],['status','状态'],['expireAt','截止时间']],studentAttendance:[['className','班级'],['studentName','学生'],['status','考勤'],['checkedAt','登记时间']],
  }
  const key=activeView.value==='courses'&&role==='STUDENT'?'coursesStudent':activeView.value==='assignments'&&role==='STUDENT'?'studentAssignments':activeView.value==='exams'&&role==='STUDENT'?'studentExams':activeView.value==='orders'&&role==='STUDENT'?'studentOrders':activeView.value==='attendance'&&role==='STUDENT'?'studentAttendance':activeView.value
  return (configs[key]||[]).map(([prop,label])=>({prop,label}))
})
const dashboardStats=computed(()=>role==='ADMIN'?[{label:'学生总数',value:adminDashboard.value?.totalStudents??'—'},{label:'在教班级',value:adminDashboard.value?.teachingClassCount??'—'},{label:'当前报名人数',value:adminDashboard.value?.currentEnrollmentCount??'—'},{label:'已支付订单',value:adminDashboard.value?.paidOrderCount??'—'},{label:'模拟订单金额',value:adminDashboard.value?`¥${Number(adminDashboard.value.simulatedOrderAmount).toFixed(2)}`:'—'},{label:'本月新增报名',value:adminDashboard.value?.newEnrollmentsThisMonth??'—'}]:role==='TEACHER'?[{label:'负责班级',value:teacherDashboard.value?.responsibleClassCount??'—'},{label:'在教班级',value:teacherDashboard.value?.inProgressClassCount??'—'},{label:'待批作业',value:teacherDashboard.value?.pendingAssignmentCount??'—'},{label:'待批简答题',value:teacherDashboard.value?.pendingExamAnswerCount??'—'}]:[{label:'已报名班级',value:studentDashboard.value?.enrolledClasses?.length??'—'},{label:'即将截止作业',value:studentDashboard.value?.upcomingAssignments?.length??'—'},{label:'未读通知',value:studentDashboard.value?.unreadNotificationCount??'—'}])

function list(data:any):Row[]{if(Array.isArray(data))return data;if(Array.isArray(data?.records))return data.records;if(Array.isArray(data?.records?.records))return data.records.records;return []}
function toUtc(value:string){return value?new Date(value).toISOString().slice(0,19):''}
function toLocal(value:string){if(!value)return '';const d=new Date(value.endsWith('Z')?value:`${value}Z`);return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,16)}

function statusLabel(value:string){const labels:Record<string,string>={ACTIVE:'启用',DISABLED:'停用',DRAFT:'草稿',PUBLISHED:'已发布',OFFLINE:'已下架',ENROLLING:'报名中',IN_PROGRESS:'教学中',FINISHED:'已结课',CANCELLED:'已取消',SCHEDULED:'待上课',COMPLETED:'已完成',PENDING:'待支付',PAID:'已支付',EXPIRED:'已过期',PRESENT:'出勤',LATE:'迟到',ABSENT:'缺勤',LEAVE:'请假',SUBMITTED:'已提交',GRADED:'已批改',CLOSED:'已关闭'};return labels[value]||value||'—'}
function statusType(value:string){if(['ACTIVE','PUBLISHED','ENROLLING','IN_PROGRESS','COMPLETED','PAID','PRESENT','GRADED'].includes(value))return 'success';if(['PENDING','SCHEDULED','SUBMITTED','DRAFT','LATE'].includes(value))return 'warning';if(['DISABLED','OFFLINE','CANCELLED','EXPIRED','ABSENT'].includes(value))return 'danger';return 'info'}
function formatTableCell(row:Row,column:any,value:any){
  const prop=String(column.property||'')
  if(prop==='attemptStatus'&&value){const labels:Record<string,string>={IN_PROGRESS:'答题中',SUBMITTED:'待批改',GRADED:'已出分'};return h(ElTag,{type:statusType(value)},{default:()=>labels[value]||value})}
  if(prop==='status'&&activeView.value==='submissions'&&!value)return h(ElTag,{type:'info'},{default:()=>'未提交'})
  if(['status','submissionStatus'].includes(prop)&&value)return h(ElTag,{type:statusType(value)},{default:()=>statusLabel(value)})
  if(prop==='classId')return row.className||classes.value.find(c=>Number(c.id)===Number(value))?.name||`班级 #${value}`
  if(prop==='courseId')return row.courseName||courses.value.find(c=>Number(c.id)===Number(value))?.name||`课程 #${value}`
  if(prop==='role'||prop==='actorRole')return roleNames[value]||value||'—'
  if(prop==='type')return ({SINGLE_CHOICE:'单选题',MULTIPLE_CHOICE:'多选题',TRUE_FALSE:'判断题',SHORT_ANSWER:'简答题'} as Record<string,string>)[value]||value||'—'
  if(prop==='difficulty')return ({EASY:'简单',MEDIUM:'中等',HARD:'困难'} as Record<string,string>)[value]||value||'—'
  if(value==null||value==='')return '—'
  if(prop==='deadline'||/At$|Time$|Date$/.test(prop))return formatTime(value)
  if(['price','amount'].includes(prop))return `¥${Number(value).toFixed(2)}${prop==='amount'?'（模拟）':''}`
  return value
}
function err(){/* API client shows the server message. */}
const fetchData=api.get
let viewLoadSequence=0
const staleView=Symbol('stale view')
async function loadPage(path:string,get=fetchData){const result=await get<Row>(path,{page:tablePage.value,size:tablePageSize.value});rows.value=list(result);tableTotal.value=Number(result?.total??rows.value.length)}
async function changeTablePage(page:number){tablePage.value=page;await loadView()}
async function changeTableSize(size:number){tablePageSize.value=size;tablePage.value=1;await loadView()}
async function loadView(){
  const sequence=++viewLoadSequence,view=activeView.value
  const api={get:async <T=any>(path:string,params?:Record<string,unknown>)=>{const data=await fetchData<T>(path,params);if(sequence!==viewLoadSequence||view!==activeView.value)throw staleView;return data}}
  const loadPageForView=(path:string)=>loadPage(path,api.get)
  loading.value=true;rows.value=[];tableTotal.value=0
  try{
    if(role==='ADMIN'){
      if(activeView.value==='overview'){
        adminDashboard.value=await api.get('/admin/dashboard');rows.value=[]
      }else if(activeView.value==='users'){await loadPageForView('/admin/users')}
      else if(activeView.value==='auditLogs'){await loadPageForView('/admin/audit-logs')}
      else if(activeView.value==='courses'){
        const courseResult=await api.get<Row>('/admin/courses',{page:tablePage.value,size:tablePageSize.value})
        rows.value=list(courseResult);courses.value=rows.value;tableTotal.value=Number(courseResult?.total??rows.value.length)
      }
      else if(activeView.value==='classes'){
        const [classResult,courseResult,userResult]=await Promise.all([api.get<Row>('/admin/classes',{page:tablePage.value,size:tablePageSize.value}),api.get('/admin/courses',{page:1,size:100}),api.get('/admin/users',{page:1,size:100})])
        courses.value=list(courseResult)
        const users=list(userResult)
        teachers.value=users.filter((x:Row)=>x.role==='TEACHER'&&x.status==='ACTIVE')
        const courseNames=new Map(courses.value.map((course:Row)=>[Number(course.id),course.name]))
        const teacherNames=new Map(users.filter((user:Row)=>user.role==='TEACHER').map((teacher:Row)=>[Number(teacher.id),teacher.realName||teacher.username]))
        classes.value=list(classResult).map((item:Row)=>({...item,courseName:item.courseName||courseNames.get(Number(item.courseId))||`课程 #${item.courseId}`,teacherName:item.teacherName||teacherNames.get(Number(item.teacherId))||`教师 #${item.teacherId}`}))
        rows.value=classes.value;tableTotal.value=Number(classResult?.total??rows.value.length)
      }
      else if(activeView.value==='classrooms'){rooms.value=list(await api.get('/admin/classrooms'));rows.value=rooms.value}
      else if(activeView.value==='schedules'){classes.value=list(await api.get('/admin/classes',{page:1,size:100}));rooms.value=list(await api.get('/admin/classrooms'));const id=selectedClassId.value||classes.value.find(c=>c.status==='IN_PROGRESS')?.id||classes.value[0]?.id;if(id){selectedClassId.value=id;scheduleForm.classId=id;rows.value=list(await api.get(`/classes/${id}/schedules`))}}
      else if(activeView.value==='orders'){await loadPageForView('/admin/orders')}
    }else if(role==='TEACHER'){
      if(activeView.value==='overview'){const [dashboard,ownedClasses]=await Promise.all([api.get<Row>('/teacher/dashboard'),api.get('/teacher/classes')]);teacherDashboard.value=dashboard;classes.value=list(ownedClasses);rows.value=classes.value}
      else if(activeView.value==='classes'){classes.value=list(await api.get('/teacher/classes'));rows.value=classes.value}
      else if(activeView.value==='students'){classes.value=list(await api.get('/teacher/classes'));const id=selectedClassId.value||classes.value[0]?.id;if(id){selectedClassId.value=id;rows.value=list(await api.get(`/teacher/classes/${id}/students`))}}
      else if(activeView.value==='schedules'){const [scheduleData,classData]=await Promise.all([api.get('/teacher/schedules'),api.get('/teacher/classes')]);classes.value=list(classData);rows.value=list(scheduleData);schedules.value=rows.value}
      else if(activeView.value==='attendance'){
        const [scheduleData,classData]=await Promise.all([api.get('/teacher/schedules'),api.get('/teacher/classes')])
        schedules.value=list(scheduleData);classes.value=list(classData)
        const available=schedules.value.filter(s=>s.status==='SCHEDULED')
        if(!available.some(s=>s.id===selectedScheduleId.value))selectedScheduleId.value=available[0]?.id
        if(selectedScheduleId.value)await loadAttendance(api.get)
        else{attendanceRoster.value=[];rows.value=[]}
      }
      else if(activeView.value==='assignments'){classes.value=list(await api.get('/teacher/classes'));const id=selectedClassId.value||classes.value[0]?.id;if(id){selectedClassId.value=id;rows.value=list(await api.get(`/teacher/classes/${id}/assignments`))}}
      else if(activeView.value==='submissions'){
        const [ownedClasses,dashboard]=await Promise.all([api.get('/teacher/classes'),api.get<Row>('/teacher/dashboard')])
        classes.value=list(ownedClasses);teacherDashboard.value=dashboard
        const assignmentGroups=await Promise.all(classes.value.map(async c=>({className:c.name,items:list(await api.get(`/teacher/classes/${c.id}/assignments`))})))
        teacherAssignments.value=assignmentGroups.flatMap(group=>group.items.map(item=>({...item,className:group.className})))
        const preferredId=dashboard.pendingAssignments?.[0]?.assignmentId
        const selectedStillOwned=teacherAssignments.value.some(item=>item.id===selectedAssignmentId.value)
        const id=selectedStillOwned?selectedAssignmentId.value:preferredId||teacherAssignments.value[0]?.id
        selectedAssignmentId.value=id
        if(id){rows.value=list(await api.get(`/teacher/assignments/${id}/submissions`));assignmentRoster.value=rows.value}
      }
      else if(activeView.value==='questions'){classes.value=list(await api.get('/teacher/classes'));courses.value=list(await api.get('/courses',{page:1,size:100}));const id=selectedCourseId.value||classes.value[0]?.courseId;if(id)selectedCourseId.value=id;rows.value=list(await api.get('/teacher/questions',id?{courseId:id}:undefined))}
      else if(activeView.value==='exams'){classes.value=list(await api.get('/teacher/classes'));rows.value=list(await api.get('/teacher/exams'))}
      else if(activeView.value==='results'){
        const [ownedClasses,allExams]=await Promise.all([api.get('/teacher/classes'),api.get('/teacher/exams')])
        classes.value=list(ownedClasses)
        const availableExams=list(allExams)
        const selectedExam=availableExams.find(e=>e.id===selectedExamId.value)
        if(selectedExam&&classes.value.some(c=>c.id===selectedExam.classId))selectedClassId.value=selectedExam.classId
        if(!classes.value.some(c=>c.id===selectedClassId.value))selectedClassId.value=classes.value[0]?.id
        teacherExams.value=availableExams.filter(e=>e.classId===selectedClassId.value)
        if(!teacherExams.value.some(e=>e.id===selectedExamId.value))selectedExamId.value=teacherExams.value.find(e=>e.status==='CLOSED')?.id||teacherExams.value.find(e=>e.status==='PUBLISHED')?.id||teacherExams.value[0]?.id
        rows.value=selectedExamId.value?list(await api.get(`/teacher/exams/${selectedExamId.value}/results`)):[]
      }
    }else{
      if(activeView.value==='overview'){const dashboard=await api.get<Row>('/students/me/dashboard');studentDashboard.value=dashboard;notificationUnread.value=dashboard.unreadNotificationCount;rows.value=[]}
      else if(activeView.value==='courses'){const [courseResult,enrollments]=await Promise.all([api.get<Row>('/courses',{page:tablePage.value,size:tablePageSize.value}),api.get('/students/me/course-enrollments')]);rows.value=list(courseResult);courses.value=rows.value;tableTotal.value=Number(courseResult?.total??rows.value.length);courseEnrollments.value=list(enrollments)}
      else if(activeView.value==='myClasses'){rows.value=list(await api.get('/students/me/classes'))}
      else if(activeView.value==='orders'){await Promise.all([loadPageForView('/orders'),api.get('/students/me/course-enrollments').then(data=>{courseEnrollments.value=list(data)})])}
      else if(activeView.value==='schedules'){const my=list(await api.get('/students/me/classes'));classes.value=my.map(c=>({...c,id:c.classId,name:c.className}));const all=await Promise.all(my.map((x:Row)=>api.get(`/classes/${x.classId}/schedules`)));rows.value=all.flatMap(list)}
      else if(activeView.value==='attendance'){rows.value=list(await api.get('/students/me/attendance'))}
      else if(activeView.value==='assignments'){rows.value=list(await api.get('/students/me/assignments'))}
      else if(activeView.value==='exams'){rows.value=list(await api.get('/students/me/exams'))}
      else if(activeView.value==='notifications'){rows.value=list(await api.get('/notifications'));notificationUnread.value=await api.get('/notifications/unread-count')}
    }
  }catch{err()}finally{if(sequence===viewLoadSequence)loading.value=false}
}
watch(activeView,()=>{tablePage.value=1;void loadView()})
watch(answers,()=>{
  if(!examStart.value||examStart.value.status!=='IN_PROGRESS')return
  answerSaveState.value='有未保存修改'
  if(answerSaveTimer)clearTimeout(answerSaveTimer)
  answerSaveTimer=setTimeout(()=>{void queueExamDraftSave().catch(()=>{})},700)
},{deep:true})
onMounted(()=>{
  if(auth.isLoggedIn) void auth.refreshCurrentUser().catch(()=>{})
  void loadView()
})

async function addCourse(){
  await api.post<Row>('/admin/courses',courseForm)
  Object.assign(courseForm,{name:'',description:'',price:0})
  ElMessage.success('课程已创建；需要开班时请到班级管理指定主讲教师')
  await loadView()
}
function createClassForCourse(row:Row){classForm.courseId=row.id;classForm.teacherId=undefined;classForm.name=row.name;activeView.value='classes'}
async function editCourse(row:Row){const {value}=await ElMessageBox.prompt('更新课程名称','修改课程',{inputValue:row.name,customClass:'app-message-box',confirmButtonText:'保存',cancelButtonText:'取消'});await api.put(`/admin/courses/${row.id}`,{name:value,description:row.description,price:row.price});await loadView()}
async function courseStatus(row:Row){const next=row.status==='PUBLISHED'?'OFFLINE':'PUBLISHED';await api.patch(`/admin/courses/${row.id}/status`,{status:next});ElMessage.success(`课程已${next==='PUBLISHED'?'上架':'下架'}`);await loadView()}
async function addTeacher(){const valid=await teacherFormRef.value?.validate().catch(()=>false);if(!valid)return;await api.post('/admin/teachers',teacherForm);Object.assign(teacherForm,{username:'',password:'',realName:''});teacherFormRef.value?.clearValidate();ElMessage.success('教师账号已创建');await loadView()}
async function toggleUser(row:Row){const status=row.status==='ACTIVE'?'DISABLED':'ACTIVE';await api.patch(`/admin/users/${row.id}/status`,{status});await loadView()}
async function addClass(){await api.post('/admin/classes',classForm);Object.assign(classForm,{courseId:undefined,teacherId:undefined,name:'',capacity:30,startDate:'',endDate:''});ElMessage.success('班级已创建');await loadView()}
async function classStatus(row:Row){const next:Record<string,string>={DRAFT:'ENROLLING',ENROLLING:'IN_PROGRESS',IN_PROGRESS:'FINISHED'};const status=next[row.status];if(!status){ElMessage.info('当前状态没有可直接执行的流转');return}await api.patch(`/admin/classes/${row.id}/status`,{status});await loadView()}
async function cancelClass(row:Row){try{await ElMessageBox.confirm('取消后班级不能重新开放报名，请确认是否继续。','取消班级',{customClass:'app-message-box',type:'warning',confirmButtonText:'确认取消',cancelButtonText:'保留班级'})}catch{return}await api.patch(`/admin/classes/${row.id}/status`,{status:'CANCELLED'});await loadView()}
function openClassEdit(row:Row){Object.assign(classEditForm,{id:row.id,courseId:row.courseId,teacherId:row.teacherId,name:row.name,capacity:row.capacity,startDate:row.startDate,endDate:row.endDate});classEditVisible.value=true}
async function saveClassEdit(){await api.put(`/admin/classes/${classEditForm.id}`,{courseId:classEditForm.courseId,teacherId:classEditForm.teacherId,name:classEditForm.name,capacity:classEditForm.capacity,startDate:classEditForm.startDate,endDate:classEditForm.endDate});classEditVisible.value=false;ElMessage.success('班级已更新');await loadView()}
async function addRoom(){await api.post('/admin/classrooms',roomForm);Object.assign(roomForm,{name:'',capacity:30});await loadView()}
function openRoomEdit(row:Row){Object.assign(roomEditForm,{id:row.id,name:row.name,capacity:row.capacity});roomEditVisible.value=true}
async function saveRoomEdit(){await api.put(`/admin/classrooms/${roomEditForm.id}`,{name:roomEditForm.name,capacity:roomEditForm.capacity});roomEditVisible.value=false;await loadView()}
async function toggleRoom(row:Row){await api.patch(`/admin/classrooms/${row.id}/status`,{status:row.status==='ACTIVE'?'DISABLED':'ACTIVE'});await loadView()}
function editSchedule(row:Row){scheduleEditId.value=row.id;Object.assign(scheduleForm,{classId:row.classId,classroomId:row.classroomId,startTime:toLocal(row.startTime),endTime:toLocal(row.endTime)});selectedClassId.value=row.classId}
async function addSchedule(){const body={classId:scheduleForm.classId,classroomId:scheduleForm.classroomId,startTime:toUtc(scheduleForm.startTime),endTime:toUtc(scheduleForm.endTime)};if(scheduleEditId.value)await api.put(`/admin/schedules/${scheduleEditId.value}`,body);else await api.post('/admin/schedules',body);scheduleEditId.value=undefined;ElMessage.success('课次已保存');if(scheduleForm.classId)selectedClassId.value=scheduleForm.classId;await loadView()}
async function openCourseOffering(course:Row){
  const enrollment=courseEnrollment(course.id)
  if(enrollment?.status==='ENROLLED'){activeView.value='myClasses';return}
  if(enrollment?.status==='PENDING'){const order=await api.get<Row>(`/orders/${enrollment.orderId}`);openPaymentDialog(order,{courseName:course.name,className:enrollment.className});return}
  selectedCourse.value=course;await loadClassesForCourse()
}
async function loadClassesForCourse(){openClasses.value=selectedCourse.value?list(await api.get('/classes',{courseId:selectedCourse.value.id})):[]}
async function enroll(classId:number){if(selectedCourse.value&&courseEnrollment(selectedCourse.value.id)){ElMessage.info('该课程已报名或已有待支付订单，请先查看我的班级或我的订单');await loadView();return;}const order=await api.post<Row>('/enrollments/orders',{classId});await loadView();const selectedClass=openClasses.value.find(item=>Number(item.id)===classId);openPaymentDialog(order,{courseName:selectedCourse.value?.name||'',className:selectedClass?.name||''})}
function openPaymentDialog(order:Row,context={courseName:'',className:''}){activePaymentOrder.value=order;paymentContext.value={courseName:context.courseName||order.courseName||'',className:context.className||order.className||''};paymentDialogVisible.value=true}
async function confirmSimulatedPayment(){const order=activePaymentOrder.value;if(!order)return;paymentLoading.value=true;try{const result=await api.post<Row>(`/orders/${order.id}/pay`);paymentDialogVisible.value=false;if(result.order?.status==='PAID')ElMessage.success('模拟支付完成，已正式加入班级');else ElMessage.warning('订单已超时，请重新报名');await loadView()}finally{paymentLoading.value=false}}
function pay(row:Row){if(!canPayOrder(row)){ElMessage.info('该课程已报名或订单已超时，请刷新订单列表');return}openPaymentDialog(row)}
async function cancelOrder(row:Row){try{await ElMessageBox.confirm(`确认取消订单 ${row.orderNo||''}？取消后需要重新报名才能加入班级。`,'取消报名订单',{customClass:'app-message-box',type:'warning',confirmButtonText:'确认取消订单',cancelButtonText:'保留订单'})}catch{return}await api.post(`/orders/${row.id}/cancel`);ElMessage.success('订单已取消');await loadView()}
async function selectTeacherClass(row:Row){selectedClassId.value=row.id;activeView.value='students';await loadView()}
async function useSchedule(row:Row){selectedScheduleId.value=row.id;activeView.value='attendance';await loadView()}
async function loadAttendance(get=fetchData){const id=selectedScheduleId.value,sequence=viewLoadSequence;attendanceRoster.value=[];rows.value=[];if(!id)return;const data=await get(`/teacher/schedules/${id}/attendance`);if(activeView.value!=='attendance'||id!==selectedScheduleId.value||sequence!==viewLoadSequence)return;attendanceRoster.value=list(data).map((x:Row)=>({...x,editStatus:x.status||undefined}));rows.value=attendanceRoster.value}
function setAllAttendance(status:string){attendanceRoster.value.forEach(student=>student.editStatus=status)}
async function saveAttendance(){if(!selectedScheduleId.value)return;const records=attendanceRoster.value.filter(x=>x.editStatus).map(x=>({studentId:x.studentId,status:x.editStatus}));if(!records.length){ElMessage.warning('请至少选择一名学生');return}await api.post(`/teacher/schedules/${selectedScheduleId.value}/attendance`,{records});ElMessage.success('考勤已保存');await loadAttendance()}
async function completeLesson(){if(!selectedScheduleId.value)return;await api.post(`/teacher/schedules/${selectedScheduleId.value}/complete`);ElMessage.success('课次已确认完成');await loadView();await loadAttendance()}
async function loadClassAnalytics(row:Row){const classId=Number(row.classId||row.id);classAnalytics.value=await api.get(`/${role==='ADMIN'?'admin':'teacher'}/classes/${classId}/analytics`)}
function openTeacherAssignment(row:Row){selectedAssignmentId.value=Number(row.assignmentId);activeView.value='submissions'}
async function openTeacherExam(row:Row){selectedExamId.value=Number(row.examId);const exams=list(await api.get('/teacher/exams'));selectedClassId.value=exams.find(e=>e.id===selectedExamId.value)?.classId;activeView.value='results'}
function openTeacherSchedule(row:Row){selectedScheduleId.value=Number(row.id);activeView.value='attendance'}
async function addAssignment(){await api.post('/teacher/assignments', {classId:assignmentForm.classId,title:assignmentForm.title,content:assignmentForm.content,deadline:toUtc(assignmentForm.deadline)});ElMessage.success('作业草稿已创建');Object.assign(assignmentForm,{title:'',content:'',deadline:''});selectedClassId.value=assignmentForm.classId;await loadView()}
async function assignmentAction(row:Row,action:string){if(action==='progress'){selectedAssignmentId.value=row.id;activeView.value='submissions';return}await api.post(`/teacher/assignments/${row.id}/${action}`);await loadView()}
function editAssignment(row:Row){Object.assign(assignmentEditForm,{id:row.id,classId:row.classId,title:row.title,content:row.content,deadline:toLocal(row.deadline)});assignmentEditVisible.value=true}
async function saveAssignmentEdit(){await api.put(`/teacher/assignments/${assignmentEditForm.id}`,{classId:assignmentEditForm.classId,title:assignmentEditForm.title,content:assignmentEditForm.content,deadline:toUtc(assignmentEditForm.deadline)});assignmentEditVisible.value=false;await loadView()}
function submitAssignment(row:Row){activeSubmissionAssignment.value=row;submissionForm.assignmentId=row.id;submissionForm.content=row.submissionContent||'';submissionDialogVisible.value=true}
async function saveAssignmentSubmission(){if(!submissionForm.assignmentId)return;if(!submissionForm.content.trim()){ElMessage.warning('请填写作业内容');return}await api.put(`/assignments/${submissionForm.assignmentId}/submission`,{content:submissionForm.content.trim()});submissionDialogVisible.value=false;ElMessage.success('作业已提交');await loadView()}
function gradeSubmission(row:Row){const submissionId=Number(row.submissionId);if(!Number.isSafeInteger(submissionId)||submissionId<=0){ElMessage.error('未找到这名学生的作业提交记录，请刷新后重试');return}gradingSubmission.value=row;Object.assign(gradeForm,{submissionId,score:Number(row.score??90),feedback:row.feedback||''});submissionGradeVisible.value=true}
async function saveSubmissionGrade(){if(!gradeForm.submissionId){ElMessage.warning('请选择一条待批改的作业提交');return}await api.post(`/teacher/submissions/${gradeForm.submissionId}/grade`,{score:Number(gradeForm.score),feedback:gradeForm.feedback});submissionGradeVisible.value=false;ElMessage.success('批改已保存');await loadView()}
async function addQuestion(){let payload;try{payload=questionPayload(questionForm)}catch(e){ElMessage.warning((e as Error).message);return}await api.post('/teacher/questions',payload);ElMessage.success('题目已添加');Object.assign(questionForm,{...emptyQuestion(),courseId:questionForm.courseId});await loadView()}
async function questionStatus(row:Row){await api.patch(`/teacher/questions/${row.id}/status`,{status:row.status==='ACTIVE'?'INACTIVE':'ACTIVE'});await loadView()}
function editQuestion(row:Row){try{Object.assign(questionEditForm,decodeQuestion(row));questionEditVisible.value=true}catch{ElMessage.error('题目数据格式异常，请联系管理员')}}
async function saveQuestionEdit(){let payload;try{payload=questionPayload(questionEditForm)}catch(e){ElMessage.warning((e as Error).message);return}await api.put(`/teacher/questions/${questionEditForm.id}`,payload);questionEditVisible.value=false;ElMessage.success('题目已更新');await loadView()}
async function addExam(){const exam=await api.post<Row>('/teacher/exams',{...examForm,startTime:toUtc(examForm.startTime),endTime:toUtc(examForm.endTime)});selectedExamId.value=exam.id;selectedClassId.value=exam.classId;ElMessage.success(`考试草稿已创建（ID ${exam.id}）`);await loadView()}
async function generateExam(row?:Row){const id=row?.id||selectedExamId.value;if(!id){ElMessage.warning('请先选择一个草稿考试');return;}await api.post(`/teacher/exams/${id}/generate`,{rules:[{...ruleForm,count:Number(ruleForm.count),score:Number(ruleForm.score)}]});ElMessage.success('组卷完成');selectedExamId.value=id;await loadView()}
async function examAction(row:Row,action:string){selectedExamId.value=row.id;if(action==='results'){selectedClassId.value=row.classId;activeView.value='results';return}await api.post(`/teacher/exams/${row.id}/${action}`);await loadView()}
async function loadResults(){const id=selectedExamId.value,sequence=viewLoadSequence;if(!id){rows.value=[];return}const data=await api.get(`/teacher/exams/${id}/results`);if(activeView.value==='results'&&id===selectedExamId.value&&sequence===viewLoadSequence)rows.value=list(data)}
async function selectResultsClass(){const id=selectedClassId.value,sequence=viewLoadSequence;teacherExams.value=[];selectedExamId.value=undefined;rows.value=[];pendingExamAnswers.value=[];if(!id)return;const data=await api.get('/teacher/exams',{classId:id});if(activeView.value!=='results'||id!==selectedClassId.value||sequence!==viewLoadSequence)return;teacherExams.value=list(data);selectedExamId.value=teacherExams.value.find(e=>e.status==='CLOSED')?.id||teacherExams.value.find(e=>e.status==='PUBLISHED')?.id||teacherExams.value[0]?.id;await loadResults()}
async function selectResultsExam(){pendingExamAnswers.value=[];await loadResults()}
async function loadPendingAnswers(){const id=selectedExamId.value,sequence=viewLoadSequence;pendingExamAnswers.value=[];if(!id)return;const data=await api.get(`/teacher/exams/${id}/answers`);if(activeView.value==='results'&&id===selectedExamId.value&&sequence===viewLoadSequence)pendingExamAnswers.value=list(data)}
function gradeExamAnswer(row:Row){gradingExamAnswer.value=row;Object.assign(examAnswerForm,{answerId:row.answerId,score:Number(row.maxScore),feedback:''});examGradeVisible.value=true}
async function saveExamGrade(){if(!examAnswerForm.answerId)return;await api.post(`/teacher/exam-answers/${examAnswerForm.answerId}/grade`,{score:Number(examAnswerForm.score),feedback:examAnswerForm.feedback});examGradeVisible.value=false;ElMessage.success('主观题已批改');await loadPendingAnswers();await loadResults()}
function queueExamDraftSave():Promise<void>{
  const session=examStart.value
  if(!session||session.status!=='IN_PROGRESS')return Promise.resolve()
  const snapshot=JSON.parse(JSON.stringify(answers)) as Record<number,any>
  const task=answerSaveQueue.catch(()=>{}).then(async()=>{
    if(examStart.value?.attemptId!==session.attemptId||!Object.keys(snapshot).length)return
    answerSaveState.value='保存中'
    await api.put(`/exams/${session.examId}/answers`,{answers:snapshot})
    if(examStart.value?.attemptId===session.attemptId)answerSaveState.value=`已保存 ${new Date().toLocaleTimeString()}`
  }).catch(error=>{if(examStart.value?.attemptId===session.attemptId)answerSaveState.value='保存失败';throw error})
  answerSaveQueue=task
  return task
}
async function saveExamDraftNow(){if(answerSaveTimer)clearTimeout(answerSaveTimer);answerSaveTimer=undefined;await answerSaveQueue.catch(()=>{});await queueExamDraftSave();ElMessage.success('答题进度已保存')}
async function startExam(row:Row){
  if(row.attemptStatus==='GRADED'){await showResult(row);return}
  if(row.attemptStatus==='SUBMITTED'){ElMessage.info('答卷已提交，等待教师批改');return}
  if(row.attemptStatus!=='IN_PROGRESS'&&!isExamOpen(row)){ElMessage.info(examButtonLabel(row));return}
  if(answerSaveTimer){clearTimeout(answerSaveTimer);answerSaveTimer=undefined;await queueExamDraftSave()}
  await answerSaveQueue.catch(()=>{});examStart.value=null
  for(const key of Object.keys(answers))delete answers[Number(key)]
  const session=await api.post<Row>(`/exams/${row.id}/start`)
  if(session.status!=='IN_PROGRESS'){examStart.value=null;ElMessage.info(session.status==='GRADED'?'考试已完成，可以查看成绩':'答卷已提交，等待教师批改');if(session.status==='GRADED')await showResult(row);return}
  for(const q of session.questions||[])answers[q.id]=session.savedAnswers?.[q.id]??(q.type==='MULTIPLE_CHOICE'?[]:null)
  examStart.value={...session,title:row.title};examDialogVisible.value=true;answerSaveState.value=Object.keys(session.savedAnswers||{}).length?'已恢复已保存的答题进度':'尚未保存'
  ElMessage.success('考试已开始，提交截止 '+formatTime(session.deadline))
}
async function submitExam(){if(!examStart.value)return;if(answerSaveTimer)clearTimeout(answerSaveTimer);answerSaveTimer=undefined;await answerSaveQueue.catch(()=>{});const session=examStart.value;if(!session)return;await api.post(`/exams/${session.examId}/submit`,{answers:{...answers}});ElMessage.success('答卷已提交');examDialogVisible.value=false;examStart.value=null;answerSaveState.value='已提交';for(const key of Object.keys(answers))delete answers[Number(key)];await loadView()}

function isExamOpen(row:Row){return row.status==='PUBLISHED'&&Date.now()>=parseUtc(row.startTime)&&Date.now()<parseUtc(row.endTime)}
function examButtonLabel(row:Row){if(row.attemptStatus==='IN_PROGRESS')return '继续考试';if(row.attemptStatus==='SUBMITTED')return '等待批改';if(row.attemptStatus==='GRADED')return '查看成绩';if(row.status==='CLOSED')return '考试已关闭';const now=Date.now();if(now<parseUtc(row.startTime))return '尚未开始';if(now>=parseUtc(row.endTime))return '考试已截止';return '开始考试'}
async function showResult(row:Row){examResult.value=await api.get(`/exams/${row.id}/result`);examResultVisible.value=true}
async function markRead(row:Row){await api.patch(`/notifications/${row.id}/read`);await loadView()}
async function showClassSchedule(row:Row){selectedClassId.value=row.classId;rows.value=list(await api.get(`/classes/${row.classId}/schedules`));activeView.value='schedules'}
function logout(){auth.logout();window.location.href='/login'}

onBeforeUnmount(()=>{viewLoadSequence++;if(answerSaveTimer)clearTimeout(answerSaveTimer);examStart.value=null})
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="sidebar-brand"><div class="brand-mark">E</div><div><strong>EduCore</strong><small>LEARNING PLATFORM</small></div></div>
      <div class="nav-label">{{ roleNames[role] }}工作空间</div>
      <nav class="side-nav">
        <button v-for="item in menus[role]" :key="item.key" class="nav-item" :class="{active:activeView===item.key}" @click="activeView=item.key"><span class="nav-dot"></span>{{ item.label }}</button>
      </nav>
      <div class="sidebar-foot"><strong>{{ displayName }}</strong>{{ roleNames[role] }} · EduCore V1.1</div>
    </aside>
    <main class="main-shell">
      <header class="topbar"><div><div class="crumb">EduCore / {{ activeMenu }}</div><h1>{{ activeMenu }}</h1></div><div class="user-area"><div class="avatar">{{ displayName.slice(0,1) }}</div><div class="user-meta"><div class="user-name">{{ displayName }}</div><div class="user-role">{{ roleNames[role] }}账号</div></div><el-button text @click="logout">退出</el-button></div></header>
      <section class="workspace">
        <div class="welcome-row"><div><h2>{{ role==='STUDENT'?'你好，'+displayName:'欢迎回来，'+displayName }}</h2><p>{{ role==='ADMIN'?'把课程、教师和教学安排放在一个工作台统一管理。':role==='TEACHER'?'查看班级进度，继续安排教学与学习反馈。':'继续你的课程和学习计划。' }}</p><small>时间按 {{ timeZoneLabel }} 显示</small></div><span class="role-tag">{{ roleNames[role] }}空间</span></div>
        <section v-if="activeView==='overview'" class="experience-guide">
          <h3>从这里开始体验</h3><p>{{ experienceGuides[role].intro }}</p>
          <div class="experience-steps"><button v-for="(step,index) in experienceGuides[role].steps" :key="step.view" type="button" @click="activeView=step.view"><span>{{ index+1 }}</span><div><strong>{{ step.title }}</strong><small>{{ step.description }}</small></div><b aria-hidden="true">→</b></button></div>
          <small class="muted-copy">三个角色的操作相互关联：教师批改后，学生可看到成绩；学生报名后，管理员和教师可看到入班记录。支付为模拟操作。</small>
        </section>
        <div v-if="activeView==='overview'" class="metric-grid"><div v-for="metric in dashboardStats" :key="metric.label" class="metric-card"><span>{{ metric.label }}</span><strong>{{ metric.value }}</strong></div></div>

        <el-card v-if="role==='ADMIN'&&activeView==='overview'&&adminDashboard" class="content-card section-gap" shadow="never">
          <template #header><div class="card-title"><div><h3>运营概览</h3><p>金额来自模拟支付流水，仅用于演示和业务流程验证</p></div></div></template>
          <div class="dashboard-columns">
            <div><h4>热门课程</h4><el-table :data="adminDashboard.popularCourses" stripe><el-table-column prop="courseName" label="课程"/><el-table-column prop="enrollmentCount" label="当前报名" width="120"/></el-table></div>
            <div><h4>班级教学情况</h4><el-table :data="adminDashboard.classTeaching" stripe><el-table-column prop="className" label="班级"/><el-table-column prop="status" label="状态" width="130"><template #default="{row}"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template></el-table-column><el-table-column prop="completedLessons" label="已完成课次" width="110"/><el-table-column prop="plannedLessons" label="计划课次" width="95"/><el-table-column prop="attendanceRate" label="出勤率" width="95"><template #default="{row}">{{ row.attendanceRate }}%</template></el-table-column></el-table></div>
          </div>
        </el-card>

        <el-card v-if="role==='TEACHER'&&activeView==='overview'&&teacherDashboard" class="content-card section-gap" shadow="never">
          <template #header><div class="card-title"><div><h3>教师工作台</h3><p>近期课表、待批任务与负责班级教学进度</p></div></div></template>
          <div class="dashboard-columns">
            <div><h4>近期课表</h4><el-empty v-if="!teacherDashboard.upcomingSchedules.length" description="暂无待上课次" :image-size="54"/><el-table v-else :data="teacherDashboard.upcomingSchedules" stripe><el-table-column prop="classId" label="班级 ID" width="95"/><el-table-column prop="startTime" label="开始时间" min-width="150"><template #default="{row}">{{ formatTime(row.startTime) }}</template></el-table-column><el-table-column prop="endTime" label="结束时间" min-width="150"><template #default="{row}">{{ formatTime(row.endTime) }}</template></el-table-column><el-table-column prop="classroomId" label="教室" width="90"/><el-table-column label="操作" width="105"><template #default="{row}"><el-button link type="primary" @click="openTeacherSchedule(row)">登记考勤</el-button></template></el-table-column></el-table></div>
            <div><h4>待批作业</h4><el-empty v-if="!teacherDashboard.pendingAssignments.length" description="暂无待批作业" :image-size="54"/><el-table v-else :data="teacherDashboard.pendingAssignments" stripe><el-table-column prop="className" label="班级" min-width="110"/><el-table-column prop="title" label="作业" min-width="120"/><el-table-column prop="studentName" label="学生" width="105"/><el-table-column prop="submittedAt" label="提交时间" min-width="150"><template #default="{row}">{{ formatTime(row.submittedAt) }}</template></el-table-column><el-table-column label="操作" width="90"><template #default="{row}"><el-button link type="primary" @click="openTeacherAssignment(row)">去批改</el-button></template></el-table-column></el-table></div>
          </div>
          <div class="dashboard-columns section-gap">
            <div><h4>待批考试简答题</h4><el-empty v-if="!teacherDashboard.pendingExams.length" description="暂无待批简答题" :image-size="54"/><el-table v-else :data="teacherDashboard.pendingExams" stripe><el-table-column prop="title" label="考试" min-width="180"/><el-table-column prop="pendingAnswerCount" label="待批题数" width="100"/><el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="primary" @click="openTeacherExam(row)">查看成绩</el-button></template></el-table-column></el-table></div>
            <div><h4>班级教学进度</h4><el-empty v-if="!teacherDashboard.classProgress.length" description="暂无负责班级" :image-size="54"/><el-table v-else :data="teacherDashboard.classProgress" stripe><el-table-column prop="className" label="班级" min-width="140"/><el-table-column prop="status" label="状态" width="110"><template #default="{row}"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template></el-table-column><el-table-column label="课次" width="100"><template #default="{row}">{{ row.completedLessons }} / {{ row.plannedLessons }}</template></el-table-column><el-table-column prop="attendanceRate" label="出勤率" width="90"><template #default="{row}">{{ row.attendanceRate }}%</template></el-table-column><el-table-column prop="averageScore" label="平均分" width="90"/><el-table-column label="分析" width="80"><template #default="{row}"><el-button link type="primary" @click="loadClassAnalytics(row)">查看</el-button></template></el-table-column></el-table></div>
          </div>
        </el-card>

        <el-card v-if="role==='STUDENT'&&activeView==='overview'&&studentDashboard" class="content-card section-gap" shadow="never">
          <template #header><div class="card-title"><div><h3>学习中心</h3><p>班级进度、待办事项和近期成绩</p></div><el-button @click="activeView='courses'">浏览课程</el-button></div></template>
          <div class="dashboard-columns">
            <div><h4>课程学习进度</h4><el-table :data="studentDashboard.classProgress" stripe><el-table-column prop="courseName" label="课程"/><el-table-column prop="className" label="班级"/><el-table-column prop="completedLessons" label="已完成课次"/><el-table-column prop="plannedLessons" label="计划课次"/><el-table-column prop="attendanceRate" label="出勤率"><template #default="{row}">{{ row.attendanceRate }}%</template></el-table-column></el-table></div>
            <div class="dashboard-side">
              <div class="dashboard-note"><span>下一次上课</span><strong v-if="studentDashboard.nextLesson">{{ studentDashboard.nextLesson.className }} · {{ formatTime(studentDashboard.nextLesson.startTime) }}</strong><strong v-else>暂无已安排课次</strong><small v-if="studentDashboard.nextLesson">教室 #{{ studentDashboard.nextLesson.classroomId }}</small></div>
              <div><h4>即将截止的作业</h4><el-empty v-if="!studentDashboard.upcomingAssignments.length" description="暂无一周内到期作业" :image-size="54"/><div v-for="item in studentDashboard.upcomingAssignments" :key="item.id" class="dashboard-list-row"><span>{{ item.title }} · {{ item.className }}</span><small>{{ formatTime(item.deadline) }}</small></div></div>
              <div><h4>即将开始的考试</h4><el-empty v-if="!studentDashboard.upcomingExams.length" description="暂无近期考试" :image-size="54"/><div v-for="item in studentDashboard.upcomingExams" :key="item.id" class="dashboard-list-row"><span>{{ item.title }} · {{ item.className }}</span><small>{{ formatTime(item.startTime) }}</small></div></div>
            </div>
          </div>
          <div class="dashboard-columns dashboard-single section-gap"><div><h4>最近考试成绩</h4><el-table :data="studentDashboard.recentResults" stripe><el-table-column prop="examTitle" label="考试"/><el-table-column prop="className" label="班级"/><el-table-column prop="score" label="成绩" width="90"/><el-table-column prop="submittedAt" label="提交时间" min-width="145" :formatter="formatTableCell"/></el-table></div></div>
        </el-card>

        <el-card v-if="classAnalytics" class="content-card section-gap" shadow="never">
          <template #header><div class="card-title"><div><h3>{{ classAnalytics.className }} · 教学分析</h3><p>考试及格线为试卷满分的 60%；分数为原始分，百分比基于有效作业、完成课次和当前在读学生</p></div><el-button text @click="classAnalytics=null">收起</el-button></div></template>
          <div class="metric-grid analytics-grid"><div class="metric-card"><span>课次进度</span><strong>{{ classAnalytics.completedLessons }} / {{ classAnalytics.plannedLessons }}</strong></div><div class="metric-card"><span>班级出勤率</span><strong>{{ classAnalytics.attendanceRate }}%</strong></div><div class="metric-card"><span>作业完成率</span><strong>{{ classAnalytics.assignmentCompletionRate }}%</strong></div><div class="metric-card"><span>班级平均分</span><strong>{{ classAnalytics.averageScore ?? '—' }}</strong></div><div class="metric-card"><span>最高 / 最低分</span><strong>{{ classAnalytics.highestScore ?? '—' }} / {{ classAnalytics.lowestScore ?? '—' }}</strong></div><div class="metric-card"><span>考试及格率</span><strong>{{ classAnalytics.examPassRate }}%</strong></div></div>
          <div class="dashboard-columns"><div><h4>考试统计</h4><el-table :data="classAnalytics.exams" stripe><el-table-column prop="title" label="考试"/><el-table-column prop="gradedAttemptCount" label="已评分人数"/><el-table-column prop="averageScore" label="平均分"/><el-table-column prop="highestScore" label="最高分"/><el-table-column prop="lowestScore" label="最低分"/><el-table-column prop="passRate" label="及格率"><template #default="{row}">{{ row.passRate }}%</template></el-table-column></el-table></div><div><h4>学生近期成绩</h4><el-table :data="classAnalytics.recentStudentResults" stripe><el-table-column prop="studentName" label="学生"/><el-table-column prop="examTitle" label="考试"/><el-table-column prop="score" label="成绩"/><el-table-column prop="submittedAt" label="提交时间" :formatter="formatTableCell"/></el-table></div></div>
        </el-card>

        <el-card v-if="role==='ADMIN'&&activeView==='users'" class="content-card section-gap" shadow="never">
          <template #header><div class="card-title"><div><h3>账号管理</h3><p>创建教师账号并管理用户状态</p></div></div></template>
          <p class="subtle">用户名需为 3–64 位，可使用英文字母、数字、点（.）、下划线（_）或短横线（-）；初始密码需为 8–72 位。</p>
          <el-form ref="teacherFormRef" :model="teacherForm" :rules="teacherRules" inline class="filters">
            <el-form-item label="用户名" prop="username"><el-input v-model="teacherForm.username" placeholder="3–64 位用户名" /></el-form-item>
            <el-form-item label="姓名" prop="realName"><el-input v-model="teacherForm.realName" placeholder="教师姓名" /></el-form-item>
            <el-form-item label="初始密码" prop="password"><el-input v-model="teacherForm.password" type="password" show-password placeholder="8–72 位密码" /></el-form-item>
            <el-button type="primary" @click="addTeacher">创建教师</el-button>
          </el-form>
          <el-table empty-text="暂无记录" :data="rows" v-loading="loading" stripe><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="110" /><el-table-column label="操作" width="110"><template #default="{row}"><el-button link type="primary" @click="toggleUser(row)">{{ row.status==='ACTIVE'?'禁用':'启用' }}</el-button></template></el-table-column></el-table>
        </el-card>

        <el-card v-if="role==='ADMIN'&&activeView==='auditLogs'" class="content-card section-gap" shadow="never">
          <template #header><div class="card-title"><div><h3>业务操作日志</h3><p>只记录操作者和请求结果，不保存密码、令牌、请求正文或答题内容</p></div></div></template>
          <el-table empty-text="暂无记录" :data="rows" v-loading="loading" stripe><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="125" show-overflow-tooltip/></el-table>
        </el-card>

        <el-card v-if="role==='ADMIN'&&activeView==='courses'" class="content-card section-gap" shadow="never">
          <template #header><div class="card-title"><div><h3>课程目录</h3><p>一门课程可以开多个教学班；在班级管理中选择所属课程和主讲教师</p></div></div></template>
          <el-form :model="courseForm" label-position="top" class="form-grid course-create-form">
            <el-form-item label="课程名称"><el-input v-model="courseForm.name" placeholder="例如：Java 入门" /></el-form-item>
            <el-form-item label="价格"><el-input-number v-model="courseForm.price" :min="0" :precision="2" /></el-form-item>
            <el-form-item label="课程说明" class="span-2"><el-input v-model="courseForm.description" placeholder="简要描述课程内容" /></el-form-item>
            <div class="course-create-actions span-2"><el-button type="primary" @click="addCourse">创建课程</el-button></div>
          </el-form>
          <el-table empty-text="暂无记录" :data="rows" v-loading="loading" stripe><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="110" /><el-table-column label="操作" width="265"><template #default="{row}"><div class="table-actions"><el-button link type="primary" @click="editCourse(row)">修改</el-button><el-button link type="primary" @click="courseStatus(row)">{{ row.status==='PUBLISHED'?'下架':'上架' }}</el-button><el-button link type="success" @click="createClassForCourse(row)">创建教学班并选老师</el-button></div></template></el-table-column></el-table>
        </el-card>

        <el-card v-if="role==='ADMIN'&&activeView==='classes'" class="content-card section-gap" shadow="never">
          <template #header><div class="card-title"><div><h3>教学班</h3><p>每个教学班关联一门课程，并指定主讲教师；列表显示课程和教师名称</p></div></div></template>
          <el-form :model="classForm" label-position="top" class="form-grid class-create-form"><el-form-item label="课程"><el-select v-model="classForm.courseId" placeholder="选择课程"><el-option v-for="c in courses" :key="c.id" :label="`${c.name} (#${c.id})`" :value="c.id" /></el-select></el-form-item><el-form-item label="主讲教师"><el-select v-model="classForm.teacherId" placeholder="选择教师"><el-option v-for="t in teachers" :key="t.id" :label="`${t.realName} (#${t.id})`" :value="t.id" /></el-select></el-form-item><el-form-item label="班级名"><el-input v-model="classForm.name" placeholder="Java 一班" /></el-form-item><el-form-item label="容量"><el-input-number v-model="classForm.capacity" :min="1" /></el-form-item><el-form-item label="开课日期"><el-date-picker v-model="classForm.startDate" value-format="YYYY-MM-DD" type="date" /></el-form-item><el-form-item label="结课日期"><el-date-picker v-model="classForm.endDate" value-format="YYYY-MM-DD" type="date" /></el-form-item><div class="course-create-actions span-2"><el-button type="primary" @click="addClass">创建班级</el-button></div></el-form>
          <el-table empty-text="暂无记录" :data="rows" v-loading="loading" stripe><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="105" /><el-table-column label="操作" min-width="330"><template #default="{row}"><div class="table-actions"><el-button link type="primary" @click="openClassEdit(row)">修改</el-button><el-button link type="primary" @click="classStatus(row)">{{ row.status==='DRAFT'?'开放报名':row.status==='ENROLLING'?'开始教学':row.status==='IN_PROGRESS'?'结束教学':'—' }}</el-button><el-button v-if="row.status==='DRAFT'||row.status==='ENROLLING'" link type="danger" @click="cancelClass(row)">取消</el-button><el-button link type="success" @click="loadClassAnalytics(row)">教学分析</el-button></div></template></el-table-column></el-table>
        </el-card>

        <el-card v-if="role==='ADMIN'&&activeView==='classrooms'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>教室资源</h3><p>排课时会校验教室容量和可用状态</p></div></div></template><el-form :model="roomForm" inline class="filters"><el-form-item label="教室名称"><el-input v-model="roomForm.name" /></el-form-item><el-form-item label="容量"><el-input-number v-model="roomForm.capacity" :min="1" /></el-form-item><el-button type="primary" @click="addRoom">添加教室</el-button></el-form><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" /><el-table-column label="操作" min-width="170"><template #default="{row}"><el-button link type="primary" @click="openRoomEdit(row)">修改</el-button><el-button link type="primary" @click="toggleRoom(row)">{{ row.status==='ACTIVE'?'停用':'启用' }}</el-button></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='ADMIN'&&activeView==='schedules'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>排课安排</h3><p>选择班级、教室和时间；系统会检查教师及教室是否冲突</p></div></div></template><el-form :model="scheduleForm" inline class="filters"><el-form-item label="班级"><el-select v-model="scheduleForm.classId" @change="selectedClassId=$event;loadView()"><el-option v-for="c in classes" :key="c.id" :label="`${c.name} (#${c.id})`" :value="c.id" /></el-select></el-form-item><el-form-item label="教室"><el-select v-model="scheduleForm.classroomId"><el-option v-for="room in rooms.filter(x=>x.status==='ACTIVE')" :key="room.id" :label="`${room.name} (${room.capacity})`" :value="room.id" /></el-select></el-form-item><el-form-item label="开始"><el-date-picker v-model="scheduleForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm" /></el-form-item><el-form-item label="结束"><el-date-picker v-model="scheduleForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm" /></el-form-item><el-button type="primary" @click="addSchedule">{{ scheduleEditId?'保存修改':'创建课次' }}</el-button><el-button v-if="scheduleEditId" @click="scheduleEditId=undefined">取消修改</el-button><el-button @click="loadView">刷新课表</el-button></el-form><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="130" /><el-table-column label="操作"><template #default="{row}"><el-button v-if="row.status==='SCHEDULED'" link type="primary" @click="editSchedule(row)">修改课次</el-button></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='ADMIN'&&activeView==='orders'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>报名订单</h3><p>查看报名状态及模拟支付金额，支付成功后学生正式入班</p></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading" stripe><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="120" /></el-table></el-card>

        <el-card v-if="role==='TEACHER'&&['overview','classes','students'].includes(activeView)" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>{{ activeView==='students'?'班级学生':'我负责的班级' }}</h3><p>查看负责班级的课程、学生名单和教学进度</p></div><el-select v-if="activeView==='students'" v-model="selectedClassId" placeholder="选班级" @change="loadView"><el-option v-for="c in classes" :key="c.id" :label="`${c.name} (#${c.id})`" :value="c.id" /></el-select></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading" stripe><el-table-column v-for="c in (activeView==='students'?columns:[{prop:'id',label:'班级 ID'},{prop:'name',label:'班级'},{prop:'courseName',label:'所属课程'},{prop:'capacity',label:'容量'},{prop:'enrolledCount',label:'已报名'},{prop:'startDate',label:'开始日期'},{prop:'status',label:'状态'}])" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="115" /><el-table-column v-if="activeView!=='students'" label="操作" min-width="190"><template #default="{row}"><el-button link type="primary" @click="selectTeacherClass(row)">查看学生</el-button><el-button link type="success" @click="loadClassAnalytics(row)">教学分析</el-button></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='TEACHER'&&activeView==='schedules'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>授课日程</h3><p>点击课次可直接登记考勤</p></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="130" /><el-table-column label="操作"><template #default="{row}"><el-button link type="primary" @click="useSchedule(row)">登记考勤</el-button></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='TEACHER'&&activeView==='attendance'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>批量考勤</h3><p>选择课次后自动读取学生名单，可批量设为出勤或缺勤；完成后考勤锁定</p></div><el-button v-if="schedules.find(s=>s.id===selectedScheduleId)?.status==='SCHEDULED'" type="success" @click="completeLesson">确认课次完成</el-button></div></template><div class="inline-form"><el-form-item label="课次"><el-select v-model="selectedScheduleId" placeholder="选择课次" @change="loadAttendance()"><el-option v-for="s in schedules.filter(s=>s.status==='SCHEDULED')" :key="s.id" :label="`${classes.find(c=>c.id===s.classId)?.name||'班级 #'+s.classId} · ${formatTime(s.startTime)}`" :value="s.id" /></el-select></el-form-item><el-button @click="loadAttendance()">刷新名单</el-button><el-button :disabled="!attendanceRoster.length" @click="setAllAttendance('PRESENT')">全员出勤</el-button><el-button :disabled="!attendanceRoster.length" @click="setAllAttendance('ABSENT')">全员缺勤</el-button><el-button type="primary" :disabled="schedules.find(s=>s.id===selectedScheduleId)?.status!=='SCHEDULED'||!attendanceRoster.length" @click="saveAttendance">保存考勤</el-button></div><el-table class="section-gap" :data="attendanceRoster" v-loading="loading"><el-table-column prop="studentId" label="学生 ID"/><el-table-column prop="realName" label="学生姓名"/><el-table-column label="考勤状态"><template #default="{row}"><el-select v-model="row.editStatus" placeholder="选择状态"><el-option v-for="v in ['PRESENT','LATE','ABSENT','LEAVE']" :key="v" :label="statusLabel(v)" :value="v" /></el-select></template></el-table-column><el-table-column prop="status" label="已保存状态"><template #default="{row}"><el-tag v-if="row.status" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag><span v-else>未登记</span></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='TEACHER'&&activeView==='assignments'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>作业管理</h3><p>选择教学中的班级创建作业草稿，发布后学生即可提交</p></div></div></template><el-form :model="assignmentForm" inline class="filters"><el-form-item label="班级"><el-select v-model="assignmentForm.classId"><el-option v-for="c in classes" :key="c.id" :label="`${c.name} (#${c.id})`" :value="c.id" /></el-select></el-form-item><el-form-item label="作业标题"><el-input v-model="assignmentForm.title" /></el-form-item><el-form-item label="截止时间"><el-date-picker v-model="assignmentForm.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm" /></el-form-item><el-form-item label="内容"><el-input v-model="assignmentForm.content" /></el-form-item><el-button type="primary" @click="addAssignment">创建草稿</el-button><el-button @click="loadView">刷新</el-button></el-form><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="120"/><el-table-column label="操作" width="260"><template #default="{row}"><el-button v-if="row.status==='DRAFT'" link type="primary" @click="editAssignment(row)">编辑草稿</el-button><el-button v-if="row.status==='DRAFT'" link type="primary" @click="assignmentAction(row,'publish')">发布</el-button><el-button v-if="row.status==='PUBLISHED'" link type="warning" @click="assignmentAction(row,'close')">关闭</el-button><el-button link type="primary" @click="assignmentAction(row,'progress')">提交情况</el-button></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='TEACHER'&&activeView==='submissions'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>作业提交进度</h3><p>默认展示待批作业；可切换班级作业查看全班花名册</p></div><el-select v-model="selectedAssignmentId" placeholder="选择作业" style="width: min(360px, 100%)" @change="loadView"><el-option v-for="item in teacherAssignments" :key="item.id" :label="`${item.title} · ${item.className} (#${item.id})`" :value="item.id" /></el-select></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading" stripe><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="125"/><el-table-column label="操作"><template #default="{row}"><el-button v-if="row.status==='SUBMITTED'" link type="primary" @click="gradeSubmission(row)">批改</el-button></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='TEACHER'&&activeView==='questions'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>课程题库</h3><p>题库已有示例题目，可继续添加或修改，组卷时按题型和难度抽取</p></div></div></template><el-form :model="questionForm" class="form-grid"><QuestionFields :form="questionForm" :course-ids="[...new Set(classes.map(c=>Number(c.courseId)))]" :course-names="Object.fromEntries(courses.map(c=>[c.id,c.name]))" /><div class="span-2"><el-button type="primary" @click="addQuestion">添加题目</el-button><el-button @click="loadView">刷新题库</el-button></div></el-form><el-table class="section-gap" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="120"/><el-table-column label="操作" min-width="170"><template #default="{row}"><el-button link type="primary" @click="editQuestion(row)">修改</el-button><el-button link type="primary" @click="questionStatus(row)">{{ row.status==='ACTIVE'?'停用':'启用' }}</el-button></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='TEACHER'&&activeView==='exams'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>考试与组卷</h3><p>创建考试 → 选择草稿考试和抽题规则 → 组卷 → 发布；已发布试卷保持固定</p></div></div></template><el-form :model="examForm" inline class="filters"><el-form-item label="班级"><el-select v-model="examForm.classId"><el-option v-for="c in classes" :key="c.id" :label="`${c.name} (#${c.id})`" :value="c.id"/></el-select></el-form-item><el-form-item label="标题"><el-input v-model="examForm.title"/></el-form-item><el-form-item label="开始"><el-date-picker v-model="examForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm"/></el-form-item><el-form-item label="结束"><el-date-picker v-model="examForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm"/></el-form-item><el-form-item label="分钟"><el-input-number v-model="examForm.durationMinutes" :min="1" :max="1440"/></el-form-item><el-button type="primary" @click="addExam">创建考试</el-button></el-form><el-form :model="ruleForm" inline class="filters"><el-form-item label="组卷题型"><el-select v-model="ruleForm.type"><el-option v-for="v in ['SINGLE_CHOICE','MULTIPLE_CHOICE','TRUE_FALSE','SHORT_ANSWER']" :key="v" :label="questionTypes[v] || difficulties[v] || v" :value="v"/></el-select></el-form-item><el-form-item label="难度"><el-select v-model="ruleForm.difficulty"><el-option v-for="v in ['EASY','MEDIUM','HARD']" :key="v" :label="questionTypes[v] || difficulties[v] || v" :value="v"/></el-select></el-form-item><el-form-item label="数量"><el-input-number v-model="ruleForm.count" :min="1"/></el-form-item><el-form-item label="单题分数"><el-input-number v-model="ruleForm.score" :min="0.01" :precision="2"/></el-form-item><el-form-item label="草稿考试"><el-select v-model="selectedExamId" placeholder="选择待组卷考试" style="width:260px"><el-option v-for="e in rows.filter(x=>x.status==='DRAFT')" :key="e.id" :label="e.title" :value="e.id"/></el-select></el-form-item><el-button @click="generateExam()">按规则组卷</el-button><el-button @click="loadView">刷新</el-button></el-form><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="130"/><el-table-column label="操作" width="230"><template #default="{row}"><el-button v-if="row.status==='DRAFT'" link type="primary" @click="generateExam(row)">组卷</el-button><el-button v-if="row.status==='DRAFT'" link type="primary" @click="examAction(row,'publish')">发布</el-button><el-button v-if="row.status==='PUBLISHED'" link type="warning" @click="examAction(row,'close')">关闭</el-button><el-button link type="primary" @click="examAction(row,'results')">成绩</el-button></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='TEACHER'&&activeView==='results'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>班级考试成绩</h3><p>先选择自己负责的班级，再选择该班级考试；主观题批改完成后公布最终成绩</p></div><div class="inline-form"><el-select v-model="selectedClassId" placeholder="选择班级" @change="selectResultsClass"><el-option v-for="c in classes" :key="c.id" :label="`${c.name} (#${c.id})`" :value="c.id"/></el-select><el-select v-model="selectedExamId" placeholder="选择考试" @change="selectResultsExam"><el-option v-for="e in teacherExams" :key="e.id" :label="e.title" :value="e.id"/></el-select><el-button :disabled="!selectedExamId" @click="loadResults">查询成绩</el-button><el-button :disabled="!selectedExamId" @click="loadPendingAnswers">待批简答题</el-button></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="130"/><el-table-column label="答题批改"><template #default="{row}"><el-tag v-if="row.status!=='GRADED'" type="warning">待完成主观题批改</el-tag><span v-else>已出分</span></template></el-table-column></el-table><div v-if="pendingExamAnswers.length" class="section-gap"><h3>待批简答题</h3><el-table :data="pendingExamAnswers"><el-table-column prop="studentName" label="学生"/><el-table-column prop="content" label="题目" min-width="200"/><el-table-column prop="answerJson" label="学生答案" min-width="200"/><el-table-column prop="maxScore" label="满分"/><el-table-column label="操作"><template #default="{row}"><el-button type="primary" link @click="gradeExamAnswer(row)">批改</el-button></template></el-table-column></el-table></div></el-card>

        <el-card v-if="role==='STUDENT'&&activeView==='courses'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>课程目录</h3><p>同一课程只能报名一个未结课的班级；已报名课程可直接查看我的班级</p></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column prop="name" label="课程" min-width="150"/><el-table-column prop="description" label="课程说明" min-width="230" show-overflow-tooltip/><el-table-column prop="price" label="模拟课程金额" width="130"/><el-table-column label="报名状态" min-width="180"><template #default="{row}"><template v-if="courseEnrollment(row.id)"><el-tag :type="courseEnrollment(row.id)?.status==='ENROLLED'?'success':'warning'">{{ courseEnrollment(row.id)?.status==='ENROLLED'?'已报名':'待支付' }}</el-tag><div class="muted-copy">{{ courseEnrollment(row.id)?.className }}</div></template><span v-else class="muted-copy">未报名</span></template></el-table-column><el-table-column label="操作" width="145"><template #default="{row}"><el-button link type="primary" @click="openCourseOffering(row)">{{ courseEnrollment(row.id)?.status==='ENROLLED'?'查看我的班级':courseEnrollment(row.id)?.status==='PENDING'?'继续支付':'查看开放班级' }}</el-button></template></el-table-column></el-table><div v-if="selectedCourse" class="section-gap"><div class="card-title"><h3>{{ selectedCourse.name }} · 开放班级</h3><el-button text @click="selectedCourse=null;openClasses=[]">收起</el-button></div><el-table :data="openClasses"><el-table-column prop="id" label="班级 ID"/><el-table-column prop="name" label="班级"/><el-table-column prop="teacherName" label="任课教师"/><el-table-column prop="capacity" label="容量"/><el-table-column prop="enrolledCount" label="已报名"/><el-table-column prop="startDate" label="开课日期"/><el-table-column label="报名"><template #default="{row}"><el-button type="primary" size="small" :disabled="!!courseEnrollment(row.courseId)" @click="enroll(row.id)">{{ courseEnrollment(row.courseId)?.status==='ENROLLED'?'该课程已报名':courseEnrollment(row.courseId)?'已有待支付订单':'报名并创建订单' }}</el-button></template></el-table-column></el-table></div></el-card>

        <el-card v-if="role==='STUDENT'&&activeView==='myClasses'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>我的班级</h3><p>模拟支付成功后正式加入班级</p></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="130"/><el-table-column label="课表"><template #default="{row}"><el-button link type="primary" @click="showClassSchedule(row)">查看课表</el-button></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='STUDENT'&&activeView==='orders'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>我的订单</h3><p>支付成功后自动正式入班；重复支付不会重复记账</p></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="130"/><el-table-column label="操作"><template #default="{row}"><el-button v-if="row.status==='PENDING'&&canPayOrder(row)" link type="primary" @click="pay(row)">模拟支付</el-button><el-tag v-else-if="row.status==='PENDING'" type="info">课程已报名或订单已失效</el-tag><el-button v-if="row.status==='PENDING'" link type="danger" @click="cancelOrder(row)">取消</el-button></template></el-table-column></el-table></el-card>

        <el-card v-if="role==='STUDENT'&&activeView==='schedules'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>我的课表</h3><p>仅聚合本人正式加入班级的课次</p></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="130"/></el-table></el-card>
        <el-card v-if="role==='STUDENT'&&activeView==='attendance'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>我的考勤</h3><p>考勤记录为只读数据</p></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="130"/></el-table></el-card>
        <el-card v-if="role==='STUDENT'&&activeView==='assignments'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>我的作业</h3><p>提交后截止前可修改，教师批改后锁定</p></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="130"/><el-table-column label="操作"><template #default="{row}"><el-button v-if="row.status==='PUBLISHED'&&row.submissionStatus!=='GRADED'" type="primary" @click="submitAssignment(row)">{{ row.submissionId?'修改提交':'提交作业' }}</el-button></template></el-table-column></el-table></el-card>
        <el-card v-if="role==='STUDENT'&&activeView==='exams'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>在线考试</h3><p>仅在考试开放时间内开始；作答自动保存，已完成的考试可查看成绩</p></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="130"/><el-table-column label="操作" min-width="160"><template #default="{row}"><el-button v-if="row.attemptStatus==='GRADED'" type="success" plain @click="showResult(row)">查看成绩</el-button><el-button v-else-if="row.attemptStatus==='SUBMITTED'" disabled>等待批改</el-button><el-button v-else-if="row.attemptStatus==='IN_PROGRESS'||isExamOpen(row)" type="primary" @click="startExam(row)">{{ examButtonLabel(row) }}</el-button><el-button v-else disabled>{{ examButtonLabel(row) }}</el-button></template></el-table-column></el-table></el-card>
        <el-card v-if="role==='STUDENT'&&activeView==='notifications'" class="content-card section-gap" shadow="never"><template #header><div class="card-title"><div><h3>站内通知</h3><p>未读 {{ notificationUnread }} 条</p></div></div></template><el-table empty-text="暂无记录" :data="rows" v-loading="loading"><el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :formatter="formatTableCell" min-width="150"/><el-table-column label="操作"><template #default="{row}"><el-button v-if="!row.readAt" link type="primary" @click="markRead(row)">标为已读</el-button><el-tag v-else type="info">已读</el-tag></template></el-table-column></el-table></el-card>

        <div v-if="showPagination" class="pagination-row"><el-pagination background layout="total, sizes, prev, pager, next" :total="tableTotal" :current-page="tablePage" :page-size="tablePageSize" :page-sizes="[10,20,50,100]" @current-change="changeTablePage" @size-change="changeTableSize"/></div>
        <el-card v-if="!((role==='ADMIN'&&['overview','users','auditLogs','courses','classes','classrooms','schedules','orders'].includes(activeView))||(role==='TEACHER'&&['overview','classes','students','schedules','attendance','assignments','submissions','questions','exams','results'].includes(activeView))||(role==='STUDENT'&&['overview','courses','myClasses','orders','schedules','attendance','assignments','exams','notifications'].includes(activeView)))" class="content-card section-gap" shadow="never"><div class="empty-state">选择左侧功能开始使用</div></el-card>
        <el-dialog v-model="paymentDialogVisible" title="确认模拟支付" width="min(500px, calc(100vw - 28px))" :close-on-click-modal="false" class="payment-dialog">
          <div class="payment-intro"><span class="dialog-kicker">报名订单</span><p>请核对订单信息，确认后将完成模拟支付并加入班级。</p></div>
          <div class="payment-amount-card"><div><span>应付金额 <small>模拟</small></span><strong>¥{{ Number(activePaymentOrder?.amount??0).toFixed(2) }}</strong></div><el-tag type="warning" effect="light">待支付</el-tag></div>
          <div class="payment-details">
            <div><span>订单编号</span><strong>{{ activePaymentOrder?.orderNo||'—' }}</strong></div>
            <div><span>课程</span><strong>{{ paymentContext.courseName||'教培课程' }}</strong></div>
            <div><span>班级</span><strong>{{ paymentContext.className||`班级 #${activePaymentOrder?.classId??'—'}` }}</strong></div>
          </div>
          <div class="payment-notice"><strong>演示环境说明</strong><p>这是模拟支付，不会产生真实扣款。确认后系统会更新订单状态，并将你加入对应班级。</p></div>
          <template #footer><div class="dialog-actions"><el-button @click="paymentDialogVisible=false">稍后支付</el-button><el-button type="primary" :loading="paymentLoading" @click="confirmSimulatedPayment">确认模拟支付</el-button></div></template>
        </el-dialog>
        <el-dialog v-model="examResultVisible" title="考试成绩" width="min(460px, calc(100vw - 28px))" class="result-dialog">
          <div class="result-heading"><span>考试名称</span><strong>{{ examResult?.examTitle||'考试' }}</strong></div>
          <div class="result-score-card"><span>本次成绩</span><strong>{{ examResult?.score??'待批改' }}<small v-if="examResult?.score!=null">分</small></strong><el-tag :type="statusType(examResult?.status||'')">{{ statusLabel(examResult?.status||'') }}</el-tag></div>
          <p v-if="examResult?.score==null" class="result-hint">答卷已提交，教师完成主观题批改后会显示最终成绩。</p>
          <template #footer><div class="dialog-actions"><el-button type="primary" @click="examResultVisible=false">关闭</el-button></div></template>
        </el-dialog>
        <el-dialog v-model="classEditVisible" title="修改班级" width="560px"><el-form :model="classEditForm" label-position="top" class="form-grid"><el-form-item label="课程"><el-select v-model="classEditForm.courseId"><el-option v-for="c in courses" :key="c.id" :label="`${c.name} (#${c.id})`" :value="c.id"/></el-select></el-form-item><el-form-item label="主讲教师"><el-select v-model="classEditForm.teacherId"><el-option v-for="t in teachers" :key="t.id" :label="`${t.realName} (#${t.id})`" :value="t.id"/></el-select></el-form-item><el-form-item label="班级名称"><el-input v-model="classEditForm.name"/></el-form-item><el-form-item label="容量"><el-input-number v-model="classEditForm.capacity" :min="1"/></el-form-item><el-form-item label="开始日期"><el-date-picker v-model="classEditForm.startDate" type="date" value-format="YYYY-MM-DD"/></el-form-item><el-form-item label="结束日期"><el-date-picker v-model="classEditForm.endDate" type="date" value-format="YYYY-MM-DD"/></el-form-item></el-form><template #footer><el-button @click="classEditVisible=false">取消</el-button><el-button type="primary" @click="saveClassEdit">保存修改</el-button></template></el-dialog>
        <el-dialog v-model="roomEditVisible" title="修改教室" width="440px"><el-form :model="roomEditForm" label-position="top"><el-form-item label="教室名称"><el-input v-model="roomEditForm.name"/></el-form-item><el-form-item label="容量"><el-input-number v-model="roomEditForm.capacity" :min="1"/></el-form-item></el-form><template #footer><el-button @click="roomEditVisible=false">取消</el-button><el-button type="primary" @click="saveRoomEdit">保存修改</el-button></template></el-dialog>
        <el-dialog v-model="assignmentEditVisible" title="修改作业草稿" width="560px"><el-form :model="assignmentEditForm" label-position="top"><el-form-item label="标题"><el-input v-model="assignmentEditForm.title"/></el-form-item><el-form-item label="截止时间"><el-date-picker v-model="assignmentEditForm.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm"/></el-form-item><el-form-item label="内容"><el-input v-model="assignmentEditForm.content" type="textarea" :rows="5"/></el-form-item></el-form><template #footer><el-button @click="assignmentEditVisible=false">取消</el-button><el-button type="primary" @click="saveAssignmentEdit">保存草稿</el-button></template></el-dialog>
        <el-dialog v-model="submissionDialogVisible" :title="activeSubmissionAssignment?.submissionId?'修改作业提交':'提交作业'" width="min(720px, 94vw)" destroy-on-close>
          <div v-if="activeSubmissionAssignment?.content" class="assignment-prompt"><strong>作业要求</strong><p>{{ activeSubmissionAssignment.content }}</p></div>
          <el-form :model="submissionForm" label-position="top"><el-form-item label="我的作业内容" required><el-input v-model="submissionForm.content" type="textarea" :rows="9" resize="vertical" maxlength="20000" show-word-limit placeholder="请在这里输入作业内容" /></el-form-item></el-form>
          <template #footer><el-button @click="submissionDialogVisible=false">取消</el-button><el-button type="primary" @click="saveAssignmentSubmission">提交作业</el-button></template>
        </el-dialog>
        <el-dialog v-model="examDialogVisible" :title="examStart?.title||'在线考试'" width="min(920px, 96vw)" top="4vh" :close-on-click-modal="false" class="exam-dialog">
          <template v-if="examStart">
            <div class="exam-toolbar"><div><strong>共 {{ examStart.questions.length }} 道题</strong><p>答题截止：{{ formatTime(examStart.deadline) }} · {{ answerSaveState }}</p></div><div class="inline-form"><el-button @click="saveExamDraftNow">保存进度</el-button><el-button type="primary" @click="submitExam">提交整份答卷</el-button></div></div>
            <div v-for="(q,index) in examStart.questions" :key="q.id" class="exam-question exam-question-card">
              <div class="exam-question-title">{{ Number(index)+1 }}. {{ q.content }} <span class="muted-copy">（{{ q.score }} 分）</span></div>
              <el-radio-group v-if="q.type==='SINGLE_CHOICE'" v-model="answers[q.id]" class="exam-options"><el-radio v-for="option in (typeof q.optionsJson==='string'?JSON.parse(q.optionsJson||'[]'):q.optionsJson||[])" :key="option.id||option" :value="option.id||option">{{ option.text||option }}</el-radio></el-radio-group>
              <el-radio-group v-else-if="q.type==='TRUE_FALSE'" v-model="answers[q.id]" class="exam-options"><el-radio :value="true">正确</el-radio><el-radio :value="false">错误</el-radio></el-radio-group>
              <el-checkbox-group v-else-if="q.type==='MULTIPLE_CHOICE'" v-model="answers[q.id]" class="exam-options"><el-checkbox v-for="option in (typeof q.optionsJson==='string'?JSON.parse(q.optionsJson||'[]'):q.optionsJson||[])" :key="option.id||option" :value="option.id||option">{{ option.text||option }}</el-checkbox></el-checkbox-group>
              <el-input v-else v-model="answers[q.id]" type="textarea" :rows="5" resize="vertical" maxlength="20000" show-word-limit placeholder="在这里输入你的答案" />
            </div>
          </template>
        </el-dialog>
        <el-dialog v-model="questionEditVisible" title="修改题目" width="620px"><el-form :model="questionEditForm" label-position="top" class="form-grid"><QuestionFields :form="questionEditForm" :course-ids="[...new Set(classes.map(c=>Number(c.courseId)))]" :course-names="Object.fromEntries(courses.map(c=>[c.id,c.name]))" /></el-form><template #footer><el-button @click="questionEditVisible=false">取消</el-button><el-button type="primary" @click="saveQuestionEdit">保存修改</el-button></template></el-dialog>
        <el-dialog v-model="submissionGradeVisible" title="作业批改" width="560px">
          <div class="review-block"><span>学生提交</span><p>{{ gradingSubmission?.content || '—' }}</p></div>
          <el-form :model="gradeForm" label-position="top"><el-form-item label="分数"><el-input-number v-model="gradeForm.score" :min="0" :max="100" :precision="2"/></el-form-item><el-form-item label="教师评语"><el-input v-model="gradeForm.feedback" type="textarea" :rows="4" placeholder="写下具体反馈"/></el-form-item></el-form>
          <template #footer><el-button @click="submissionGradeVisible=false">取消</el-button><el-button type="primary" @click="saveSubmissionGrade">保存批改</el-button></template>
        </el-dialog>
        <el-dialog v-model="examGradeVisible" title="简答题批改" width="560px">
          <div class="review-block"><span>{{ gradingExamAnswer?.studentName }} · {{ gradingExamAnswer?.content }}</span><p>{{ gradingExamAnswer?.answerJson || '学生未作答' }}</p><small>本题满分 {{ gradingExamAnswer?.maxScore }} 分</small></div>
          <el-form :model="examAnswerForm" label-position="top"><el-form-item label="分数"><el-input-number v-model="examAnswerForm.score" :min="0" :max="Number(gradingExamAnswer?.maxScore||0)" :precision="2"/></el-form-item><el-form-item label="教师评语"><el-input v-model="examAnswerForm.feedback" type="textarea" :rows="4" placeholder="写下具体反馈"/></el-form-item></el-form>
          <template #footer><el-button @click="examGradeVisible=false">取消</el-button><el-button type="primary" @click="saveExamGrade">保存批改</el-button></template>
        </el-dialog>
      </section>
    </main>
  </div>
</template>
