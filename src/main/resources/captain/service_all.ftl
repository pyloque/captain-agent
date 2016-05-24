<#include "layout.ftl">

<#macro content>
<div class="panel panel-success">
	<div class="panel-heading">All Services[version=${version}]</div>
	<div class="panel-body">
		<ul class="list-group">
			<#list services?keys as name>
			<li class="list-group-item"><span class="badge">${services[name]}</span><a href="/service/set/?name=${name}">${name}</a></li>
			</#list>
		</ul>
	</div>
</div>
</#macro>

<@render />