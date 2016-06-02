<#include "layout.ftl">

<#macro content>
<#include "config_show.ftl">
<div class="panel panel-success">
	<div class="panel-heading">
		Service<a href="/agent/"> ${services.name}</a> [version=${services.version}]
		<a href="javascript:void(0)"
            class="btn btn-link btn-sm pull-right"
            data-toggle="popover"
            data-html="true"
            data-placement="left"
            data-content="<a class='btn btn-danger' href='/agent/service/unwatch?name=${services.name}'>Unwatch Now</a>"><span class="glyphicon glyphicon-remove"></span></a>
	</div>
	<div class="panel-body">
		<ul class="list-group">
			<#list services.items as service>
			<li class="list-group-item">${service.host}:${service.port?c}</span></li>
			</#list>
		</ul>
	</div>
</div>
</#macro>

<@render />