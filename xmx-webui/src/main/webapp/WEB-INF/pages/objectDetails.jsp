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

    function callSetField(fid) {
        var value = document.getElementById("value_" + fid).value;
        window.location = "${pageContext.request.contextPath}/setObjectField/${refpath}?fid=" + fid+ "&value=" + encodeURIComponent(value);
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

    function loadFullJson(fid) {
	    var url = "${pageContext.request.contextPath}/getFullJson/${refpath}" +
            (fid === undefined ? "" : "?fid=" + fid);
        window.open(url, '_blank');
    }

</script>
</head>

<h1>Object Details</h1>

<table border="2">
    <tr>
        <td><b>RefPath</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${refpath}"/>"/></td>
    </tr>
    <tr>
        <td><b>Class</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${className}"/>"/></td>
    </tr>
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
                        <td><img src="/images/alert.red.png" alt="Warning!"/></td>
                        <td><input type="button" onclick="loadFullJson();" value="<fmt:message key='jsonTruncated.loadFull'/>" ></td>
                    </tr>
                </table>
            </c:if>
        </td>
    </tr>
</table>

<c:set var="elementsHeader" value="${details.array ? 'Array Elements' : 'Fields'}" />
<c:set var="elementsColumn" value="${details.array ? 'Index' : 'Name'}" />
<h2 style="display: inline-block">${elementsHeader}</h2>
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
        <td>${elementsColumn}</td>
        <td>Value</td>
        <td></td>
    </tr>
    </thead>

<c:if test="${details.array}">
    <c:forEach items="${details.arrayPage.pageElements}" var="val" varStatus="st">
        <c:set var="elementIdx" value="${details.arrayPage.pageStart + st.index}"/>
        <tr>
            <td><a href="${pageContext.request.contextPath}/getObjectDetails/${refpath}.${elementIdx}">${elementIdx}</a></td>
            <c:set var="elementValue" value="${
                valKind == 'SMART' ? val.smartTextValue :
                valKind == 'JSON' ? val.jsonValue : val.toStringValue
            }"/>
            <c:set var="truncated" value="${val.jsonTruncated &&
                (valKind == 'JSON' || (valKind == 'SMART' && val.smartUsesJson))
            }"/>
            <td><input type="text" id="value_${elementIdx}" value="${fn:escapeXml(elementValue)}"/></td>
            <td class="supportsTruncationWarning">
                <input type="button" onclick="callSetField('${elementIdx}');" value="Set">
                <c:if test="${truncated}">
                    <table class="truncationWarning" title="<fmt:message key='jsonTruncated.tooltip'/>">
                        <tr>
                            <td><img src="/images/alert.red.png" alt="Warning!"/></td>
                            <td><input type="button" onclick="loadFullJson('${elementIdx}');" value="<fmt:message key='jsonTruncated.loadFull'/>" ></td>
                        </tr>
                    </table>
                </c:if>
            </td>
        </tr>
    </c:forEach>
</c:if>
<c:if test="${not details.array}">
<c:forEach items="${details.fieldsByClass}" var="entry">
    <tr>
        <td colspan="3"><b>${entry.key}</b></td> <%--className--%>
    </tr>
  <c:forEach items="${entry.value}" var="fieldInfo">
    <tr>
        <td><a href="${pageContext.request.contextPath}/getObjectDetails/${refpath}.${fieldInfo.id}">${fieldInfo.name}</a></td>
        <c:set var="fieldValue" value="${
            valKind == 'SMART' ? fieldInfo.text.smartTextValue :
            valKind == 'JSON' ? fieldInfo.text.jsonValue : fieldInfo.text.toStringValue
        }"/>
        <c:set var="truncated" value="${fieldInfo.text.jsonTruncated &&
            (valKind == 'JSON' || (valKind == 'SMART' && fieldInfo.text.smartUsesJson))
        }"/>
        <td><input type="text" id="value_${fieldInfo.id}" value="${fn:escapeXml(fieldValue)}"/></td>
        <td class="supportsTruncationWarning">
            <input type="button" onclick="callSetField('${fieldInfo.id}');" value="Set">
            <c:if test="${truncated}">
                <table class="truncationWarning" title="<fmt:message key='jsonTruncated.tooltip'/>">
                    <tr>
                        <td><img src="/images/alert.red.png" alt="Warning!"/></td>
                        <td><input type="button" onclick="loadFullJson('${fieldInfo.id}');" value="<fmt:message key='jsonTruncated.loadFull'/>" ></td>
                    </tr>
                </table>
            </c:if>
        </td>
    </tr>
  </c:forEach>
</c:forEach>
</c:if>
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