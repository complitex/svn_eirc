$(function(){
    $(".service_picker_form ").on("keyup", ".filter-td input[type='text']", function(event){
        var input = $(this);
        if(event.which == $.ui.keyCode.ENTER){
            input.closest(".service_picker_form").find(".service-picker-find").click();
        }
    });
});