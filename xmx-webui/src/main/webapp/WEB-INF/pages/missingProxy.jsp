<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- Copyright © 2018 Andrey Mogilev. All rights reserved. --%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Error: missing proxy</title>

</head>
<body>

<h1>
    The requested refpath requires the root object to be proxied, but that proxy is missing now
    <a href="/getObjectDetails/${refpath}?sid=${sid}">Refresh</a>
</h1>

</body>
</html>