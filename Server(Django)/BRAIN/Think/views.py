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
		b64Image = data["ImageData"]
		print ("Obtained an image.")

		rollno = getPersonFromImage(b64Image)
		data = getPersonDetails(rollno)
		return HttpResponse(json.dumps(data))


	except Exception as e:
		print ("Failed - " + str(e))


	return HttpResponse(json.dumps(person))

def getPersonFromImage(b64Image):

	# Face recognition
	rollno = 1210314802

	return rollno

def getPersonDetails(rollno):

	# Return all details of the
	# person with the given rollno
	# in form of a dict


	person = {
	'Name' : 'cvbnm',
	'Sem'  : '100',
	'Branch' : 'CSE',
	'Rollno' : '1',
	'Att' : float(110.0)
	}


	return person
