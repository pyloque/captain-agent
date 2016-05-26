<#include "layout.ftl">

<#macro content>
<#include "config_show.ftl">
<div class="panel panel-success">
	<div class="panel-heading"><a href="/service/">Service ${name}</a> [version=${version}]</div>
	<div class="panel-body">
		<ul class="list-group">
			<#list services as service>
			<li class="list-group-item">${service.key()}</li>
			</#list>
		</ul>
	</div>
</div>
</#macro>

<@render />