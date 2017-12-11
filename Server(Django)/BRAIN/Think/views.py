from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.http import require_http_methods
from django.views.decorators.csrf import csrf_exempt
import json


# Create your views here.

@csrf_exempt
@require_http_methods(["GET", "POST"])

def getPersonInfo(request):

	try:
		data = request.POST
		print (data["ImageData"])

	except Exception as e:
		print ("Failed - " + str(e))
	person = {
	'Name' : 'Student Name',
	'Sem'  : '10',
	'Branch' : 'CSE',
	'Rollno' : '121',
	'Att' : float(100.0)
	}

	return HttpResponse(json.dumps(person))