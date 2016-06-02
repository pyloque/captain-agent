<#include "layout.ftl">

<#macro content>
<#include "config_show.ftl">
<div class="panel panel-success">
	<div class="panel-heading">All Services</div>
	<div class="panel-body">
		<ul class="list-group">
			<#list services as name>
			<li class="list-group-item"><a href="/agent/service/?name=${name}">${name}</a></li>
			</#list>
		</ul>
	</div>
</div>
<div class="panel panel-success">
	<div class="panel-heading">
		All KeyValues
	</div>
	<div class="panel-body">
		<ul class="list-group">
			<#list kvs as key>
			<li class="list-group-item"><a href="/agent/kv/?key=${key}">${key}</a></li>
			</#list>
		</ul>
	</div>
</div>
</#macro>

<@render />