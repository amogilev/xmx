<%@ page import="com.gilecode.xmx.ui.dto.ExtendedObjectInfoDto" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<fmt:setBundle basename="messages"/>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>XMX Object Details</title>

<c:url var="css" value="/css" />
<c:url var="images" value="/images" />

<link href="${css}/main.css" rel="stylesheet" type="text/css" />
<link href="${css}/xmx-icons.css" rel="stylesheet" type="text/css" />

<c:url var="curUrl" value="" />

<%
    String permaRefPath = ((ExtendedObjectInfoDto)request.getAttribute("details")).getPermaRefPath();
    if (permaRefPath != null) {
        pageContext.setAttribute("permaUrl", pageContext.getServletContext().getContextPath() +
                "/getObjectDetails/" + java.net.URLEncoder.encode(permaRefPath, "UTF-8") + "?sid=");
    }
    String refpath = (String) request.getAttribute("refpath");
    pageContext.setAttribute("refpathEnc", java.net.URLEncoder.encode(refpath, "UTF-8"));
%>

    <script type="text/javascript">

    <c:if test="${details.array}">
    window.onload = function () {
        var inPageNum = document.getElementById("inPageNum");
        inPageNum.onkeydown = function (e) {
            if (e.keyCode == 13) {
                var pageNum = parseInt(inPageNum.value);
                if (pageNum == inPageNum.value && pageNum >=0 && pageNum < ${details.arrayPage.totalPages}) {
                    document.getElementById("inPageNum").value = pageNum;
                    submitPageReload();
                } else {
                    alert("Invalid array elements page: " + inPageNum.value);
                }
            }
        }
    };

    function setArrayPage(pageNum) {
        var curPageNum = ${details.arrayPage.pageNum};
        var maxPageNum = ${details.arrayPage.totalPages} - 1;
        if (pageNum != curPageNum && pageNum >= 0 && pageNum <= maxPageNum) {
            document.getElementById("inPageNum").value = pageNum;
            submitPageReload();
        }
    }
    </c:if>

    function submitPageReload() {
        document.getElementById("frmPageNav").submit();
    }

    function callSetElement(eid) {
        var value = document.getElementById("value_" + eid).value;
        window.location = "${pageContext.request.contextPath}/setObjectElement/${refpathEnc}?elementId=" + eid +
            "&value=" + encodeURIComponent(value) + "&sid=${sid}";
	}

    function invokeMethod(methodId) {
        var inputs = document.getElementById("m" + methodId).getElementsByTagName("input");
        var form = document.createElement("form");
        form.method = 'POST';
        form.action = '${pageContext.request.contextPath}/invokeMethod/${refpathEnc}';
        form.style = 'display: none;';
        form.target = '_blank';
        addFormData(form, 'methodId', methodId);
        for (var i = 0; i < inputs.length; i++) {
            var argInput = inputs[i];
            addFormData(form, 'arg', argInput.value);
        }
        addFormData(form, "sid", "${sid}");
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
	    var url = "${pageContext.request.contextPath}/getFullJson/${refpathEnc}?sid=${sid}" +
            (fid === undefined ? "" : "&fid=" + fid);
        window.open(url, '_blank');
    }

</script>
</head>
<body>

<h1>Object Details</h1>

<table border="2">
    <tr>
        <td><span class="label-with-tip" title="<fmt:message key='refpath.tooltip'/>">RefPath</span></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${refpath}"/>"/></td>
    </tr>
<c:if test="${details.permaRefPath != null}">
    <tr>
        <td><span class="label-with-tip" title="<fmt:message key='permaLink.tooltip'/>">PermaLink</span></td>
        <td width="90%">
            <a href="${permaUrl}"><c:out value="${details.permaRefPath}"/></a>
        </td>
    </tr>
</c:if>
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
                        <td><img src="${images}/alert.red.png" alt="Warning!"/></td>
                        <td><input type="button" onclick="loadFullJson();" value="<fmt:message key='jsonTruncated.loadFull'/>" ></td>
                    </tr>
                </table>
            </c:if>
        </td>
    </tr>
</table>

<form id="frmPageNav" action="${curUrl}">
    <input type="hidden" name="sid" value="${sid}" />

    <c:set var="elementsHeader" value="${details.array ? 'Array Elements' : 'Fields'}" />
    <c:set var="elementsColumn" value="${details.array ? 'Index' : 'Name'}" />
    <h2 style="display: inline-block">${elementsHeader}</h2>
    <span style="margin-left: 50px">
        (Display values as
        <c:set var="kinds" value="<%=com.gilecode.xmx.ui.ValuesDisplayKind.values()%>"/>
        <c:forEach var="k" items="${kinds}">
            <label><input name="valKind" type="radio" value="${k}"
                          onchange="submitPageReload();" <c:if test="${k==valKind}">checked="checked"</c:if>/>
                    ${k.displayName}
            </label>
        </c:forEach>
        )
    </span>
    <br style="clear: left" />

    <table border="2">
        <thead>
        <tr>
            <td>${elementsColumn}</td>
            <td>Value</td>
            <td style="text-align: center"><i title="<fmt:message key='input.field.tooltip'/>" class="icon-question-circle-o"></i></td>
        </tr>
        </thead>

    <c:if test="${details.array}">
        <c:forEach items="${details.arrayPage.pageElements}" var="val" varStatus="st">
            <c:set var="elementIdx" value="${details.arrayPage.pageStart + st.index}"/>
            <tr>
                <td><a href="${pageContext.request.contextPath}/getObjectDetails/${refpathEnc}.${elementIdx}?sid=${sid}">${elementIdx}</a></td>
                <c:set var="elementValue" value="${
                    valKind == 'SMART' ? val.smartTextValue :
                    valKind == 'JSON' ? val.jsonValue : val.toStringValue
                }"/>
                <c:set var="truncated" value="${val.jsonTruncated &&
                    (valKind == 'JSON' || (valKind == 'SMART' && val.smartUsesJson))
                }"/>
                <td><input type="text" id="value_${elementIdx}" value="${fn:escapeXml(elementValue)}"/></td>
                <td class="supportsTruncationWarning">
                    <input type="button" onclick="callSetElement('${elementIdx}');" value="Set">
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
            <td><a href="${pageContext.request.contextPath}/getObjectDetails/${refpathEnc}.${fieldInfo.id}?sid=${sid}"
                class="${fieldInfo.staticField ? 'static' : ''}">${fieldInfo.name}</a></td>
            <c:set var="fieldValue" value="${
                valKind == 'SMART' ? fieldInfo.text.smartTextValue :
                valKind == 'JSON' ? fieldInfo.text.jsonValue : fieldInfo.text.toStringValue
            }"/>
            <c:set var="truncated" value="${fieldInfo.text.jsonTruncated &&
                (valKind == 'JSON' || (valKind == 'SMART' && fieldInfo.text.smartUsesJson))
            }"/>
            <td><input type="text" id="value_${fieldInfo.id}" value="${fn:escapeXml(fieldValue)}"/></td>
            <td class="supportsTruncationWarning">
                <input type="button" onclick="callSetElement('${fieldInfo.id}');" value="Set">
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
    <c:if test="${details.array}">
        <c:set var="ap" value="${details.arrayPage}"/>
        <div id="arrPager" style="margin-top: 3px">
            <c:set var="prevNavEnabled" value="${ap.pageNum > 0}" />
            <c:set var="nextNavEnabled" value="${ap.pageNum < ap.totalPages - 1}" />
            <span id="prevPage" class="${prevNavEnabled ? 'pageNav' : 'pageNavDisabled'}" onclick="setArrayPage(${ap.pageNum - 1});">&lt;&lt; </span>
            Page <input type="text" name="arrPage" id="inPageNum" value="${ap.pageNum}" size="1"> of <span>${ap.totalPages}</span>
            <span id="nextPage" class="${nextNavEnabled ? 'pageNav' : 'pageNavDisabled'}" onclick="setArrayPage(${ap.pageNum + 1});"> &gt;&gt;</span>
        </div>
    </c:if>
</form>

<h2>Methods</h2>
<table border="2">
    <thead>
    <tr>
        <td>Name</td>
        <td>Signature</td>
        <td style="text-align: center"><i title="<fmt:message key='input.method.tooltip'/>" class="icon-question-circle-o"></i></td>
    </tr>
    </thead>
    
<c:forEach items="${details.methodsByClass}" var="entry">
    <tr>
        <td colspan="3"><b>${entry.key}</b></td> <%--className--%>
    </tr>
  <c:forEach items="${entry.value}" var="methodInfo">
    <tr>
        <td class="${methodInfo.staticMethod ? 'static' : ''}">${methodInfo.name}</td>
        <td id="m${methodInfo.id}">
            ${methodInfo.nameTypeSignature}(
            <c:forEach items="${methodInfo.parameters}" var="p" varStatus="status">
                <input type="text" value="${p}"/>
                <c:if test="${not status.last}">,</c:if>
            </c:forEach>
            )
        </td>
        <td>
            <input type="button" onclick="invokeMethod('${methodInfo.id}');" value="Invoke"/>
        </td>
    </tr>
  </c:forEach>
</c:forEach>
</table>

</body>
</html>