<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>XMX Method Invocation Result</title>
</head>
<body>

<h1>Method invoked on object #${objectId}</h1>

<h2>Result:</h2>
<table>
<tr><td>${result}</td></tr>
</table>

<h3><a href="${pageContext.request.contextPath}/getObjectDetails?objectId=${objectId}">Back to object details</a></h3>


</body>
</html>