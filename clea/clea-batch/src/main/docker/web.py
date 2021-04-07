import os
from subprocess import Popen, run, TimeoutExpired, PIPE
from flask import Flask, jsonify, abort, request, Response, render_template, send_file, redirect
from flask_autoindex import AutoIndex

app = Flask(__name__)

files_index = AutoIndex(app, os.getenv('CLEA_BATCH_CLUSTER_OUTPUT_PATH',"/tmp/v1"), add_url_rules=False)

@app.route('/batch', methods=['POST'])
def respond():
    proc = Popen("java -jar ../clea-batch.jar", stdout=PIPE, stderr=PIPE, shell=True)
    try:
        outs, errs = proc.communicate(timeout=180)
    except TimeoutExpired:
        proc.kill()
        abort(500, description="The timeout is expired!")

    if errs:
        abort(500, description=errs.decode('utf-8'))

    if proc.returncode != 0:
        return Response(outs.decode('utf-8'),500)
    return outs.decode('utf-8')

@app.route('/')
@app.route('/bucket')
def redirect_bucket():
    return redirect("/bucket/v1/", code=302)

# Custom indexing
@app.route('/bucket/v1/')
@app.route('/bucket/v1/<path:path>')
def autoindex(path='.'):
    return files_index.render_autoindex(path)

@app.errorhandler(400)
def bad_request(error):
    return jsonify(success=False, message=error.description), 400

@app.errorhandler(500)
def server_error(error):
    return jsonify(success=False, message=error.description) , 500

