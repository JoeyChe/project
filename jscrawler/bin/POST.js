importPackage(Packages.webtools);

var data = new Object();
data["k1"] = "valu e1";
data["k2"] = "val=k";

var post = new Object();
post["url"] = "http://9gag.com";
post["charset"] = "utf-8";
post["method"] = "POST";
post["data"] = data;


var http = new HttpManager();
var content = http.execute(JSON.stringify(post));
