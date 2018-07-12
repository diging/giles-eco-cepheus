<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>

<script>
$( document ).ready(function() {
    setTimeout(function(){
        window.location.reload(1);
     }, 3000); 
});
</script>

<h1>Current Request Progress</h1>

<b>Currently working on:</b> ${request.filename}<br>
<b>Document ID:</b> ${request.documentId}
<p>

<b>Status:</b> ${status}

<div class="progress">
  <div class="progress-bar" role="progressbar" aria-valuenow="${progress}" aria-valuemin="0" aria-valuemax="100" style="width: ${progress}%;">
    ${progress}%
  </div>
</div>