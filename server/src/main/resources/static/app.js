"use strict";

// Define your client-side logic here.
<script type="text/javascript">
$('form').submit(function(e) {
    var $form = $(this);
    var url = $form.attr('action');

    $.ajax({
           type: 'POST',
           url: url,
           data: $form.serialize(),
           success: function(data) {
               $('div.alert').remove();
               $form.after('<br><div class="alert alert-success" role="alert">' + data + '</div>');
           },
           error: function(data) {
               $('div.alert').remove();
               $form.after('<br><div class="alert alert-danger" role="alert">Invalid input!</div>');
           }
         });

    e.preventDefault();
});
