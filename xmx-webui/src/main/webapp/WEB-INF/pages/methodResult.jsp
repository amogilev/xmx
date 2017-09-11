<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>XMX Method Invocation Result</title>
</head>
<body>

<h1>Method invoked on object ${refpath}</h1>

<h2>Result:</h2>
<table border="2">
    <tr>
        <td><b>toString()</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="${fn:escapeXml(result.toStringValue)}"/></td>
    </tr>
    <tr>
        <td><b>JSON</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="${fn:escapeXml(result.jsonValue)}"/></td>
    </tr>
</table>

</body>
</html>