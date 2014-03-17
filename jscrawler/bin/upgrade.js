importPackage(Packages.webtools);

var credentials = new Object();
credentials["credentials"] = new Object();
credentials["credentials"]["username"] = "admin";
credentials["credentials"]["password"] = "a10";

var auth = new Object();
auth["url"] = "https://192.168.105.224/auth";
auth["charset"] = "utf-8";
auth["method"] = "POST";
auth["contenttype"] = "application/json";
auth["data"] = JSON.stringify(credentials);

var post = new Object();
post["url"] = "http://www.baidu.com";
post["charset"] = "utf-8";
post["method"] = "POST";
post["contenttype"] = "application/octet-stream";
post["file"] = "/usr/bin/ls";
post["timeout"] = 5000;


var http = new HttpManager();

print(JSON.stringify(post));
var content = http.execute(JSON.stringify(post));
//print(content);
