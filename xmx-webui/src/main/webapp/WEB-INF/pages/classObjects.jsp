<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<body>
<h1>List of ${className} managed objects</h1>
<table border="2">
    <thead>
    <tr>
        <td>ID of object</td>
        <td>Object's json representation</td>
        <td>Object's toString() representation</td>
    </tr>
    </thead>
    <c:forEach items="${objects}" var="object">
    <tr>
            <td>${object.objectId} (<a href="${pageContext.request.contextPath}/getObjectDetails?objectId=${object.objectId}">Details</a>)</td>
            <td>${object.jsonRepresentation}</td>
            <td>${object.strRepresentation}</td>
        </tr>
    </c:forEach>
</table>
</body>
</html>