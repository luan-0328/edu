import collections
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get("EDUCORE_BASE_URL", "http://localhost").rstrip("/")
PASSWORD = os.environ.get("EDUCORE_SMOKE_PASSWORD")
if not PASSWORD:
    raise SystemExit("EDUCORE_SMOKE_PASSWORD is required")


def call(method, path, role=None, body=None, query=None):
    url = BASE + path
    if query:
        url += "?" + urllib.parse.urlencode(query)
    headers = {"Accept": "application/json"}
    if role:
        headers["Authorization"] = "Bearer " + tokens[role]
    data = None
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            raw = response.read().decode("utf-8", "replace")
            status = response.status
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", "replace")
        status = error.code
    except Exception as error:
        return 0, {"transportError": type(error).__name__ + ": " + str(error)}
    try:
        parsed = json.loads(raw) if raw else {}
    except ValueError:
        parsed = {"raw": raw[:300]}
    return status, parsed


tokens = {}
for role, username in (("admin", "demo-admin"), ("teacher", "demo-teacher"), ("student", "demo-student")):
    status, result = call("POST", "/api/auth/login", body={"username": username, "password": PASSWORD})
    token = result.get("data", {}).get("accessToken") if isinstance(result, dict) else None
    if status != 200 or not token:
        raise SystemExit(f"Login failed for {role}: HTTP {status}, code={result.get('code') if isinstance(result, dict) else 'n/a'}")
    tokens[role] = token


def role_for(path):
    if "/admin/" in path:
        return "admin"
    if "/teacher/" in path:
        return "teacher"
    return "student"


def fill_path(path, missing=False):
    replacement = "999999" if missing else "1"
    for name in ("id", "classId", "scheduleId", "courseId"):
        path = path.replace("{" + name + "}", replacement)
    return path


status, spec = call("GET", "/v3/api-docs")
if status != 200:
    raise SystemExit(f"OpenAPI fetch failed: HTTP {status}")

read_results, write_results = [], []
for raw_path, path_item in spec["paths"].items():
    for method, operation in path_item.items():
        method = method.upper()
        if method not in {"GET", "POST", "PUT", "PATCH", "DELETE"}:
            continue
        role = role_for(raw_path)
        if raw_path.startswith("/api/auth/"):
            role = None
        if method == "GET":
            path = fill_path(raw_path)
            query = {}
            for param in operation.get("parameters", []):
                if param.get("in") == "query":
                    name = param["name"]
                    if name == "courseId": query[name] = 1
                    elif name == "page": query[name] = 1
                    elif name == "size": query[name] = 3
                    elif name == "classId": query[name] = 1
            actual, response = call(method, path, role, query=query)
            passed = 200 <= actual < 300
            read_results.append((method, path, actual, passed, response.get("code") if isinstance(response, dict) else "n/a"))
        else:
            path = fill_path(raw_path, missing=True)
            actual, response = call(method, path, role, body={})
            passed = actual in {400, 401, 403, 404, 409, 422}
            write_results.append((method, path, actual, passed, response.get("code") if isinstance(response, dict) else "n/a"))

denials = [
    ("GET", "/api/admin/dashboard", "teacher"),
    ("GET", "/api/admin/users", "teacher"),
    ("GET", "/api/teacher/dashboard", "student"),
    ("GET", "/api/teacher/classes/1/students", "student"),
    ("GET", "/api/teacher/exams/1/results", "student"),
]
denial_results = []
for method, path, role in denials:
    actual, response = call(method, path, role)
    denial_results.append((method, path, actual, actual == 403, response.get("code") if isinstance(response, dict) else "n/a"))

all_results = read_results + write_results + denial_results
failures = [row for row in all_results if not row[3]]
counts = collections.Counter(row[2] for row in all_results)
print(f"OpenAPI operations: {len(read_results) + len(write_results)} (GET {len(read_results)}, write {len(write_results)})")
print(f"Role-denial checks: {len(denial_results)}")
print("HTTP status counts: " + ", ".join(f"{code}={n}" for code, n in sorted(counts.items())))
print(f"Passed: {len(all_results) - len(failures)}; failed: {len(failures)}")
for result in failures:
    print("FAIL", *result)
if failures:
    sys.exit(1)
