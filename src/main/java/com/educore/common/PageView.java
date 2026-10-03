package com.educore.common;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
public record PageView<T>(List<T> records,long page,long size,long total) {
 public static <S,T> PageView<T> from(Page<S> page,List<T> records){return new PageView<>(records,page.getCurrent(),page.getSize(),page.getTotal());}
}
