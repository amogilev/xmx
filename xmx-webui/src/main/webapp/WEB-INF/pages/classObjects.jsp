<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<fmt:setBundle basename="messages"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>XMX Managed Objects</title>
<link href="./css/main.css" rel="stylesheet" type="text/css" />
<script type="text/javascript">
    function loadFullJson(objectId) {
        var url = "${pageContext.request.contextPath}/getFullJson?objectId=" + objectId;
        window.open(url, '_blank');
    }
</script>
</head>

<body>
<h1>List of ${className} managed objects</h1>
<table border="2" width="85%">
    <thead>
    <tr>
        <td>ID</td>
        <td>Value</td>
    </tr>
    </thead>
    <c:forEach items="${objects}" var="object">
        <tr>
            <td>${object.objectId} (<a href="${pageContext.request.contextPath}/getObjectDetails/$${object.objectId}">Details</a>)</td>
            <td class="supportsTruncationWarning">
                <c:out value="${object.text.smartTextValue}"/>
                <c:if test="${object.text.smartUsesJson && object.text.jsonTruncated}">
                    <table class="truncationWarning" title="<fmt:message key='jsonTruncated.tooltip'/>">
                        <tr>
                            <td><img src="/images/alert.red.png" alt="Warning!"/></td>
                            <td><input type="button" onclick="loadFullJson(${object.objectId});" value="<fmt:message key='jsonTruncated.loadFull'/>" ></td>
                        </tr>
                    </table>
                </c:if>
            </td>
        </tr>
    </c:forEach>
</table>
</body>
</html>