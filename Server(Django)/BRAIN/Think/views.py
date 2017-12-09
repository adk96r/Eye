from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.http import require_http_methods
from django.views.decorators.csrf import csrf_exempt
import json


# Create your views here.

@csrf_exempt
@require_http_methods(["GET", "POST"])

def getPersonInfo(request):

	person = {
	'Name' : 'jaffa',
	'Sem'  : '8',
	'Branch' : 'CSE',
	'Rollno' : '1210314802',
	'Att' : float(85.0)
	}

	return HttpResponse(json.dumps(person))