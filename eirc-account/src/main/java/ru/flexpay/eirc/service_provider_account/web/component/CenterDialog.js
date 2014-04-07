(function(){
    var dialogId = '${dialogId}';
    var dialog = $('#'+dialogId).closest(".ui-dialog");
    
    //calculate dialog coordinates in document coordinate system
    var x = ($(window).width() - dialog.width())/2 + $(window).scrollLeft();
    var y = ($(window).height() - dialog.height())/2 + $(window).scrollTop();
    dialog[0].style.left = x+"px";
    dialog[0].style.top = y+"px";
})();