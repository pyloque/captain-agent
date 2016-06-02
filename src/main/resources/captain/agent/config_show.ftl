<div class="panel panel-success">
	<div class="panel-heading">Agent Configuration</div>
	<div class="panel-body">
		<ul class="list-group">
			<li class="list-group-item"><b class="text-success">ini</b> file=${config.inifile()}</li>
			<li class="list-group-item"><b class="text-success">bind</b> addr=0.0.0.0:${config.bindPort()?c}</li>
			<li class="list-group-item"><b class="text-success">keepalive</b> value=${config.keepAlive()?c}</li>
			<li class="list-group-item"><b class="text-success">watch</b> interval=${config.interval()?c}</li>
			<li class="list-group-item"><b class="text-success">shmfile</b> path=${config.shmfile()}</li>
		</ul>
	</div>
</div>