importPackage(Packages.webtools);

var credentials = new Object();
credentials["credentials"] = new Object();
credentials["credentials"]["username"] = "";
credentials["credentials"]["password"] = "";

var auth = new Object();
auth["url"] = "https://192.168.212.203/axapi/v3/auth";
auth["charset"] = "utf-8";
auth["method"] = "POST";
auth["contenttype"] = "application/json";
auth["data"] = JSON.stringify(credentials);

var post = new Object();
post["url"] = "https://192.168.212.203/axapi/v3/upgrade/hd/sec";
post["charset"] = "utf-8";
post["method"] = "POST";
post["contenttype"] = "application/octet-stream";
post["file"] = "/usr/bin/ls";
post["timeout"] = 500;


var http = new HttpManager();

print(JSON.stringify(auth));
var content = http.execute(JSON.stringify(auth));
print(content);
var info = eval("(" + content + ")");
print(info["authresponse"]["signature"]);
var headers = new Object();
headers["Authorization"] = "A10 " + info["authresponse"]["signature"];
post["headers"] = headers;
print(JSON.stringify(post));
print("###################################################\n");
var content = http.execute(JSON.stringify(post));
print(content);
