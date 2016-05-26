<div class="panel panel-success">
	<div class="panel-heading">Configuration<a class="btn btn-link" href="/service/config/">Edit</a></div>
	<div class="panel-body">
		<ul class="list-group">
			<li class="list-group-item"><b class="text-success">ini</b> file=${config.inifile()}</li>
			<li class="list-group-item"><b class="text-success">bind</b> addr=${config.bindHost()}:${config.bindPort()?c}</li>
			<li class="list-group-item"><b class="text-success">redis</b> addr=${config.redisHost()}:${config.redisPort()?c} db=${config.redisDb()?c}</li>
			<li class="list-group-item"><b class="text-success">watch</b> interval=${config.interval()?c}</li>
		</ul>
	</div>
</div>