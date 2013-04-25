/* Before we begin, prepare to use nano to write to CouchDB, create a reference to particular
 * database we'll be using. Prepare to use querystring to parse the POST data, and of course
 * prepare to use the HTTP library
 */

var    nano = require('nano')('http://127.0.0.1:5984')
,    tracks = nano.use('tracks')
,        qs = require('querystring')
,      http = require('http')

/* Normally, we won't serve any pages, but for testing purposes, here's some HTML to serve up
 */

dummyHTML =
  '<html><head><title>Node.js test page for Cycletracks API</title></head>' +
  '<body>' +
  '<form method="post">' +
  'JSON:<input type="text" name="json">' +
  '<input type="submit">' +
  '</form></body></html>';

http.createServer(function (req, res) {
  //Don't even touch other forms of requests.
  if (req.method === "POST") {
    var body = "";
    //Grab POST data one chunk at a time and aggregate.
    req.on('data', function(chunk) { body += chunk; });
    req.on('end', function() {
      //Filter out the odd empty request.
      if (body) {
        /* Instrumentation:
         * console.log('Received via POST: ' + body);
         */

        //CouchDB expects a genuine JSON object
        var couchInput = qs.parse(body);

        /* Instrumentation:
         * console.log('Updating CouchDB with ' + JSON.stringify(couchInput));
         */

        //Create a more-or-less unique name for 
        var epoch = new Date().getTime().toString();
        console.log(epoch);
        tracks.insert(couchInput, epoch, function (err, body) { if (err) console.error(err); });
      }
    });
  }
  res.writeHead(200);
  res.end(dummyHTML);
}).listen(1337, '127.0.0.1');
/* Last bit of instrumentation
 * console.log('Server running at http://127.0.0.1:1337/');
 */
