/******************************************************************************
 * jpeg2video Web Client
 * 
 * 
 * 
 * ## Released under MIT License
 * ## Copyright (c) 2020 Hernan Perrone (hernan.perrone@gmail.com)
 *****************************************************************************/

 /**
  * @brief Object to wrap a Dash Media Player instance
  * API doc: http://cdn.dashjs.org/latest/jsdoc/module-MediaPlayer.html
  */
 function DashPlayerWrapper($scope) {
	// The reference to the DASH Media Player instance (keep it private)
	var dash_player;
	var player_state = "none";

	this.reset = function(video_elem_id, url) {
		// On reset, destroy the current instance and create a new
		if (dash_player != undefined) {
			dash_player.reset();
			dash_player = undefined;
			player_state = "none";
		}

		if (url == undefined || url == null || url == "") {
			return;
		} 

		dash_player = dashjs.MediaPlayer().create();	
		dash_player.on("canPlay", function() {
			if (status == "none") {
				player_state = "ready";
				$scope.$apply();
			}
		});
		dash_player.on("playbackEnded", function() {
			player_state = "end";
			$scope.$apply();
		});
		dash_player.on("playbackPaused", function() {
			player_state = "paused";
			$scope.$apply();
		});
		dash_player.on("playbackPlaying", function() {
			player_state = "playing";
			$scope.$apply();
		});

		dash_player.initialize(
			document.querySelector("#" + video_elem_id),
			url, false);
			dash_player.preload();

	};

	this.play = function() {
		if (dash_player != undefined) {
			dash_player.play();
		}
	};

	this.pause = function() {
		if (dash_player != undefined) {
			dash_player.pause();
		}
	};

	this.seek = function(time) {
		if (dash_player != undefined) {
			dash_player.seek(time);
		}
	};

	this.is_ready = function() {
		return player_state == "ready" || player_state == "playing" ||
				player_state == "paused";
	}

	this.is_playing = function() {
		return player_state == "playing";
	}

	this.is_paused = function() {
		return player_state == "paused";
	}

	this.is_end = function() {
		return player_state == "end";
	}

	return this;
}

function StreamItem(obj) {
	/**
	 * Updates the current object data with the values from in_obj
	 */
	this.update = function(inobj) {
		this.dir = inobj.dir == undefined ? this.dir : inobj.dir;
		this.title = inobj.title == undefined ? this.title : inobj.title;
		this.desc = inobj.desc == undefined ? this.desc : inobj.desc;
	}

	/**
	 * Compares if the id of the current StreamItem matches the one form obj
	 */
	this.is_same_id = function(obj) {
		return obj.dir == this.dir;
	}

	this.update(obj);

	return this;
}

/**
 * This is a very basic AngularJS controller for managing the stream list and 
 * the current selected stream.
 * 
 * On each 5 seconds, the controller queries the server for updates on the
 * stream list (vidfeed/streams.json).
 * 
 * Also, the controller instantiates a DashMediaPlayerWrapper to control the
 * playback of the current selected stream. 
 */
angular.module('jpeg2videoApp', ['ngAnimate'])
.controller('jpeg2videoController', function($scope, $http, $interval) {
	$scope.stream_curr = undefined;
	$scope.stream_list = [];

	$scope.stream_select = function(stream_sel) {
		$scope.stream_curr = stream_sel;
		$scope.player.reset("video_player", 
			"vidfeed/" + $scope.stream_curr.dir + "/stream.mpd");
	}

	/**
	 * Internal function to poll the stream list and update it.
	 */
	var stream_list_refresh = function() {
		$http.get('vidfeed/streams.json').then(function(response) {
			// Note: AngularJS keeps track of the elements on the stream_list
			// by inserting metadata. For this reason, it is necessary to update
			// each single element, one by one.
			// Known limitation of the current implementation: just adds/updates
			// but do not removes elements if no longer included in the server
			// response.  
			for (var strid in response.data) {
				var updated = false;

				if (response.data[strid].dir == undefined) {
					continue;
				}

				for (var i = 0; i < $scope.stream_list.length; i++) {
					if ($scope.stream_list[i].is_same_id(response.data[strid])) {
						// Update our entry
						$scope.stream_list[i].update(response.data[strid]);
						updated = true;
						break;
					}
				}

				if (!updated) {
					// This is a new entry, add to our list.
					$scope.stream_list.push(new StreamItem(
							response.data[strid]));
				}
			} 

			if ($scope.utils.is_empty($scope.stream_curr) &&
					$scope.stream_list.length > 0) {
				$scope.stream_select($scope.stream_list[0])
			}
		});
	};

	$interval(stream_list_refresh, 5000);

	/**
	 * Some utilities to simplify the dynamic code within the HTML. 
	 */
	$scope.utils = {
		is_empty: function(val) { 
			return val == undefined || val == null || val == []; 
		}
	};

	$scope.player = new DashPlayerWrapper($scope);

	stream_list_refresh();
});

