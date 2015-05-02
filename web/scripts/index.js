Parse.initialize("GcExsXYqD8fWlkRbmw6Zv0st5beMr2ZekQOwDUsY",
                 "rlIX84rwLOAp9qa9CV2SpHh4rnN1OqLLpkZoH2oS");

var currentUser;

$('#btnLogin').click(function() {
    var id = $('#username').val();
    var password = $('#password').val();
    var hash = CryptoJS.SHA256(password)+"";

    Parse.User.logIn(id, hash, {
        success: function(user) {
            currentUser = Parse.User.current();
            window.location = "./home.html"
        },
        error: function(user, error) {
            console.log("Fail");
            console.log(error);
        }
    });
});