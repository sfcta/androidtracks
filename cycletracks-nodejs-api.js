/* Before we begin, prepare to use nano to write to CouchDB, create a reference to particular
 * database we'll be using. Prepare to use querystring to parse the POST data, and of course
 * prepare to use the HTTP library
 */

var qs = require('querystring');
var http = require('http');
var tracks = require('nano')('https://ougeremseratontsedisamen:QLbVnxIDp0cHSqGgtOiMDyCR@openbike.cloudant.com/openbike');

http.createServer(function (req, res) {
  // Only process POST requests (below, if non-POST, we show a debug form)
  if (req.method === "POST") {
    var body = "";
    // Grab POST data one chunk at a time and aggregate.
    req.on('data', function(chunk) { body += chunk; });
    req.on('end', function() {
      // Filter out the odd empty request.
      if (body) {
        // console.log( 'Received via POST: ' + body );
        // decodedBody = decodeURIComponent( body );
        // console.log('Decoded: ' + decodedBody);

        try {
          // CouchDB expects a genuine JSON object
          var couchInput = qs.parse(body);
        } catch ( err ) {
          console.log('Error: JSON parsing');
          res.writeHead( 500, {'Content-Type': 'application/json'});
          res.write( '{ "status":"error", "msg":"json parse error" }' );
          return;
        }

        // console.log('Updating CouchDB with ' + JSON.stringify(couchInput));

        // Create a (probably) unique name for 
        var epoch = new Date().getTime().toString() + '_' + ( Math.floor( Math.random() * 1000 ) + 1 );
        console.log(epoch);
        tracks.insert(couchInput, epoch, function (err, body) { if (err) console.error(err); });
        res.writeHead(200, {'Content-Type': 'application/json'});
        res.write( '{"status":"success"}' );
      }
    });
  } else {
    // Not a POST request, show a debug form where you can paste JSON
    dummyHTML =
      '<html><head><title>Node.js test page for Cycletracks API</title></head>' +
      '<body>' +
      '<form method="post">' +
      'JSON:<input type="text" name="json">' +
      '<input type="submit">' +
      '</form></body></html>';
    res.writeHead(200, {'Content-Type': 'text/html'} );
    res.end(dummyHTML);
  }
}).listen(1337);

console.log('Server running on port 1337');
 