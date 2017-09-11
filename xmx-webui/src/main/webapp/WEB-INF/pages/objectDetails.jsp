<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<fmt:setBundle basename="messages"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>XMX Object Details</title>

<link href="${pageContext.request.contextPath}/css/main.css" rel="stylesheet" type="text/css" />

<script type="text/javascript">

    function changeValuesDisplay(newValKind) {
        if ('${valKind}' != newValKind) {
            window.location = "${pageContext.request.contextPath}/getObjectDetails/${refpath}?valKind=" + newValKind;
        }
    }

    function callSetField(fieldId) {
        var value = document.getElementById("value_" + fieldId).value;
        window.location = "${pageContext.request.contextPath}/setObjectField/${refpath}?fieldId=" + fieldId + "&value=" + encodeURIComponent(value);
	}

    function invokeMethod(methodId) {
        var inputs = document.getElementById("m" + methodId).getElementsByTagName("input");
        var form = document.createElement("form");
        form.method = 'POST';
        form.action = '${pageContext.request.contextPath}/invokeMethod/${refpath}';
        form.style = 'display: none;';
        form.target = '_blank';
        addFormData(form, 'methodId', methodId);
        for (var i = 0; i < inputs.length; i++) {
            var argInput = inputs[i];
            addFormData(form, 'arg', argInput.value);
        }
        document.body.appendChild(form);
        form.submit();
    }

    function addFormData(form, name, value) {
        var input = document.createElement('input');
        input.type = "text";
        input.name = name;
        input.value = value;
        form.appendChild(input);
    }

    function loadFullJson(fieldId) {
	    var url = "${pageContext.request.contextPath}/getFullJson/${refpath}" +
            (fieldId === undefined ? "" : "?fieldId=" + fieldId);
        window.open(url, '_blank');
    }

</script>
</head>

<h1>Details of object ${refpath} (${className})</h1>

<table border="2">
    <tr>
        <td><b>toString()</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${details.text.toStringValue}"/>"/></td>
    </tr>
    <tr>
        <td><b>JSON</b></td>
        <td width="90%" class="supportsTruncationWarning">
            <input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${details.text.jsonValue}"/>"/>
            <c:if test="${details.text.jsonTruncated}">
                <table class="truncationWarning" title="<fmt:message key='jsonTruncated.tooltip'/>">
                    <tr>
                        <td><img src="./images/alert.red.png" alt="Warning!"/></td>
                        <td><input type="button" onclick="loadFullJson();" value="<fmt:message key='jsonTruncated.loadFull'/>" ></td>
                    </tr>
                </table>
            </c:if>
        </td>
    </tr>
</table>

<h2 style="display: inline-block">Fields</h2>
<span style="margin-left: 50px">
    (Display values as
    <form action="#" style="display: inline">
    	<c:set var="kinds" value="<%=com.gilecode.xmx.ui.ValuesDisplayKind.values()%>"/>
        <c:forEach var="k" items="${kinds}">
            <label><input name="vdKind" type="radio" value="${k}"
                          onchange="changeValuesDisplay('${k}');" <c:if test="${k==valKind}">checked="checked"</c:if>/>
                    ${k.displayName}
            </label>
        </c:forEach>
    </form>)
</span>
<br style="clear: left" />

<table border="2">
    <thead>
    <tr>
        <td>Name</td>
        <td>Value</td>
        <td></td>
    </tr>
    </thead>
    
<c:forEach items="${details.fieldsByClass}" var="entry">
    <tr>
        <td colspan="3"><b>${entry.key}</b></td> <%--className--%>
    </tr>
  <c:forEach items="${entry.value}" var="fieldInfo">
    <tr>
        <td>${fieldInfo.name}</td>
        <c:set var="fieldValue" value="${
            valKind == 'SMART' ? fieldInfo.text.smartTextValue :
            valKind == 'JSON' ? fieldInfo.text.jsonValue : fieldInfo.text.toStringValue
        }"/>
        <c:set var="truncated" value="${fieldInfo.text.jsonTruncated &&
            (valKind == 'JSON' || (valKind == 'SMART' && fieldInfo.text.smartUsesJson))
        }"/>
        <td><input type="text" id="value_${fieldInfo.id}" value="${fn:escapeXml(fieldValue)}"/></td>
        <td class="supportsTruncationWarning">
            <input type="button" onclick="callSetField(${fieldInfo.id});" value="Set">
            <c:if test="${truncated}">
                <table class="truncationWarning" title="<fmt:message key='jsonTruncated.tooltip'/>">
                    <tr>
                        <td><img src="./images/alert.red.png" alt="Warning!"/></td>
                        <td><input type="button" onclick="loadFullJson(${fieldInfo.id});" value="<fmt:message key='jsonTruncated.loadFull'/>" ></td>
                    </tr>
                </table>
            </c:if>
        </td>
    </tr>
  </c:forEach>
</c:forEach>
</table>

<h2>Methods</h2>
<table border="2">
    <thead>
    <tr>
        <td>Name</td>
        <td>Signature</td>
        <td></td>
    </tr>
    </thead>
    
<c:forEach items="${details.methodsByClass}" var="entry">
    <tr>
        <td colspan="3"><b>${entry.key}</b></td> <%--className--%>
    </tr>
  <c:forEach items="${entry.value}" var="methodInfo">
    <tr>
        <td>${methodInfo.name}</td>
        <td id="m${methodInfo.id}">
            ${methodInfo.nameTypeSignature}(
            <c:forEach items="${methodInfo.parameters}" var="p" varStatus="status">
                <input type="text" id="m${methodInfo.id}_p${status.index}" value="${p}"/>
                <c:if test="${not status.last}">,</c:if>
            </c:forEach>
            )
        </td>
        <td>
            <input type="button" onclick="invokeMethod(${methodInfo.id});" value="Invoke"/>
        </td>
    </tr>
  </c:forEach>
</c:forEach>
</table>

</body>
</html>