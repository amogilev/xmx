<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<body>
<h1>List of applications and their classes</h1>
<table border="2">
    <thead style="font-style: italic">
    <tr>
        <td>Class</td>
        <td>ClassLoader ID</td>
        <td>Objects number</td>
    </tr>
    </thead>
    <c:forEach items="${managedAppsClassesMap}" var="entry">
        <tr>
            <td colspan="3"><b>${entry.key}</b></td> <%--appName--%>
        </tr>

        <c:forEach items="${entry.value}" var="classInfo">
            <tr>
                <td>${classInfo.className}</td>
                <td>${classInfo.classLoaderId}</td>
                <td>${classInfo.numberOfObjects} (<a href="${pageContext.request.contextPath}/getClassObjects?classId=${classInfo.id}&className=${classInfo.className}">Look</a>)</td>
            </tr>
        </c:forEach>
    </c:forEach>
</table>
</body>
</html>