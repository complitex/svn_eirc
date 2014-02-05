$(function(){
    $(".service_picker_form .filter-td input[type='text']").live("keyup", function(event){
            var input = $(this);
            if(event.which == $.ui.keyCode.ENTER){
                input.closest(".service_provider_account_picker_form").find(".service-provider-account-picker-find").click();
            }
        });        
});