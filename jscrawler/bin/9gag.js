importPackage(Packages.webtools);


var request = new Object();
request["url"] = "http://9gag.com/";
request["charset"] = "utf-8";
request["method"] = "GET";

var post = new Object();
post["url"] = "http://www.bumpimage.com/dataInterface.php";
post["charset"] = "utf-8";
post["method"] = "POST";

var http = new HttpManager();
var content = http.execute(JSON.stringify(request));
var root = HtmlXPath.build(content);
var nodes = root.evaluateXPath("//article");

function get_time()
{
    var d = new java.util.Date();
    return new String(Math.floor(d.getTime() / 1000));
    var year = d.getYear() + 1900;
    var month = d.getMonth() + 1;
    var day = d.getDate();
    var hours = d.getHours();
    var minutes = d.getMinutes();
    var seconds = d.getSeconds();

    var time = new String(year);
    if (month < 10) {
        time += "0";
    }

    time += month;
    if (day < 10) {
        time += "0";
    }

    time += day;
    if (hours < 10) {
        time += "0";
    }

    time += hours;
    if (minutes < 10) {
        time += "0";
    }

    time += minutes;
    if (seconds < 10) {
        time += "0";
    }

    time += seconds;
    return time;
}

function process_div(div)
{
    try {
    var imgs = div.evaluateXPath("//img[@class='badge-item-img']");
    var gif_imgs = div.evaluateXPath("//img[@class='badge-item-animated-img']");
    var gif = null;
    if (gif_imgs != null) {
        gif = new String(gif_imgs[0].evaluateXPath("/@src")[0].getString());
    }

    var jpg = null;
    var title = null;
    if (imgs != null) {
        var jpg = new String(imgs[0].evaluateXPath("/@src")[0].getString());
        var title = new String(imgs[0].evaluateXPath("/@alt")[0].getString());
    }
    

    if (null == jpg || null == title) {
        print("Bad records.\n");
        return;
    }

    var data = new Object();
    data["title"] = title;
    if (gif != null) {
        jpg = gif;
    }

    data["image_url"] = jpg;
    data["obtain_time"] = get_time();
    data["from_name"] = "9gag";
    data["from_url"] = new String(request["url"]);

    post["data"] = data;
    print(JSON.stringify(post) + "\n");
    var result = http.execute(JSON.stringify(post));
    print(result + "\n");
    } catch (e) {
        print(e);
    }
}

function process_article(node)
{
    var divs = node.evaluateXPath("//div[@class='badge-post-container post-container']");

    if (divs != null && divs.length > 0) {
        process_div(divs[0]);
    }
}

for (i = nodes.length - 1; i >= 0; i--) {
    process_article(nodes[i]);
}
