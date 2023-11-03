$(function() {
	console.log("ready")
	//
	$("#input_1").focus();
	//
	//	$("#input_1").keydown(function(e) {
	//		console.log("keydown:" + e.keyCode);
	//		if (e.keyCode == 13) {
	//			showDialog($("#input_1").val());
	//		}
	//	});

	$("#bt_confirm").on('click', function() {
		// 
		//		var url = "http://192.168.239.197:9527";
		$.post("/set", {
			url: $("#input_1").val()
		}, function(result) {
			console.log("=====result:" + result);
			var tip = result;
			if (result.slice(0, 1) == "{") {
				var res = JSON.parse(result);
				tip = res.msg;
			}
			showDialog(tip);
		});
	});
	//
	$('#dialogs').on('click', '.weui-dialog__btn', function() {
		$(this).parents('.js_dialog').fadeOut(200);
		$(this).parents('.js_dialog').attr('aria-hidden', 'true');
		$(this).parents('.js_dialog').removeAttr('tabindex');
	});
})

function showDialog(text) {
	var $iosDialog1 = $('#iosDialog1');
	$("#dialog_text").text(text);
	$iosDialog1.fadeIn(200);
	$iosDialog1.attr('aria-hidden', 'false');
	$iosDialog1.attr('tabindex', '0');
	$iosDialog1.trigger('focus');
}