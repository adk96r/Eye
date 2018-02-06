# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.http import require_http_methods
from django.views.decorators.csrf import csrf_exempt
import json
import face_recognition
# before the feature

# Receives the request which has the image as base 64 and calls
# the method getPersonFromImage to get the person's roll number.
# Then uses this roll number to return the data.
# NO NEED TO MODIFY THIS FUNCTION.
@csrf_exempt
@require_http_methods(["GET", "POST"])
def getPersonInfo(request):
	try:
		data = request.POST
		b64Image = data["ImageData"]
		print ("Obtained an image.")

		rollno = getPersonFromImage(b64Image)
		print ("Obtained the roll number.")

		data = getPersonDetails(rollno)
		print ("Obtained the person's data ..")

		return HttpResponse(json.dumps(data))

	except Exception as e:
		print ("Failed - " + str(e))

	return HttpResponse(404)

# Gets an image encoded as base64 string and returns the roll number
# of the person in the image.
# NO NEED TO MODIFY THIS FUNCTION.
def getPersonFromImage(b64_image):

	image_file_name = "image.jpg"

	# Convert the base 64 encoded string into an image file and save it.
	convertB64toImageAndSave(b64_image, image_file_name)
	print ("Saved the image.")

	# Now open the file and get the encodings as a list.
	unknown_encodings = getEncodingsFromImageFile(image_file_name)
	print ("Got the unknown image's encodings.")

	# Get the known encodigns.
	known_encodings = getKnownEncodings(CSV_filename)
	print ("Got the known encodings.")

	# Now compare the encodings to get the rollnumber ( adding 1210314801 because
	# the compare function returns the index of the encoding in the known encodings list
	# that best matches the unknown encoding ).
	delta = compare(known_encodings, unknown_encodings)
	print ("Got the delta = " + delta)

	possible_roll_number = 1210314802 + delta
	print ("Possible roll number - " + possible_roll_number)

	return possible_roll_number

# Receives a roll number and returns the details associated with that
# roll number as a JSON.
def getPersonDetails(rollno):

	# These are just sample details.
	person = {
		'Name' : 'cvbnm',
		'Sem'  : '100',
		'Branch' : 'CSE',
		'Rollno' : '1',
		'Att' : float(110.0)
	}

	return person

# Return the csv as a list.
def getKnownEncodings():

	encodings = list()
	try:
		with open('Encodings.csv', 'r') as c:
			reader = csv.reader(c, delimiter=';', dialect='excel')
			for r in reader:
				encodings.append([float(r[x]) for x in range(1, len(r))])
		print ("Loaded the known encodings list from the csv. The size is " + len(list))
	except Exception as e:
		print ("Exception in getKnownEncodings " + e)

	return encodings


# Take a b64 file and convert it and save it as <imageFileName>.jpg.
def convertB64toImageAndSave(b64_string, image_file_name):
	try:
		#imgdata takes the decoded base64 string into imgdata
		#image_file_name is the file name where the image is stored
		#imgdata is returned
        try:
            imgdata = base64.b64decode(b64_string)
        except:
            print("Couldnt decode")

		print ("Decoded the unknown image.")

		with open(image_file_name,'wb') as fo:
			fo.write(imgdata)
		print ("Saved the unknown image")

	except Exception as e:
		print ("Exception in convertB64toImageAndSave " + e)

# Open the given image file and extract the 128 encodings and return them.
def getEncodingsFromImageFile(image_file_name):

	try:
		#Just loading the image and encoding the Unknown image values
		unknown_image = face_recognition.load_image_file(image_file_name)
		print ("Loaded the unknown image.")

		unknown_encodings = face_recognition.face_encodings(unknown_image)[0]
		print ("Obtained the unknown encodings")
		return unknown_encodings

	except Exception as e:
		print ("Exception in getEncodingsFromImageFile " + e)

	return []

# Compare the face with the given list of encodings and return the possible
# entry in the known encodings. Return -1 if nothing's been found.
def compare(known_encodings, unknown_encodings):
	try:
		results = fr.compare_faces([known_encodings], unknown_encodings, tolerance = tol)
		print ("Compared faces. Got results of len " + len(results))

		for m in range(0, len(results)):
			if results[m]:
				return m
	except Exception as e:
		print ("Exception in compare " + e)
	return -1
