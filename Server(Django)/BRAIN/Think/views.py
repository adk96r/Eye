from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.http import require_http_methods
from django.views.decorators.csrf import csrf_exempt
import json

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
		data = getPersonDetails(rollno)
		return HttpResponse(json.dumps(data))

	except Exception as e:
		print ("Failed - " + str(e))

	return HttpResponse(json.dumps(person))

# Gets an image encoded as base64 string and returns the roll number
# of the person in the image.
# NO NEED TO MODIFY THIS FUNCTION.
def getPersonFromImage(b64_image):

	image_file_name = "image.jpg"
	
	# Convert the base 64 encoded string into an image file and save it.
	convertB64toImageAndSave(b64_image, image_file_name)

	# Now open the file and get the encodings as a list.
	unknown_encodings = getEncodingsFromImageFile(image_file_name)

	# Get the known encodigns.
	known_encodings = getKnownEncodings(CSV_filename)

	# Now compare the encodings to get the rollnumber ( adding 1210314801 because
	# the compare function returns the index of the encoding in the known encodings list
	# that best matches the unknown encoding ).
	possible_roll_number = 1210314801 + compare(known_encodings, unknown_encodings)

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


# FUNCTIONS TO WRITE

# Take a b64 file and convert it and save it as <imageFileName>.jpg.
def convertB64toImageAndSave(b64_string, image_file_name){}

# Open the given image file and extract the 128 encodings and return them.
def getEncodingsFromImageFile(image_file_name){}

# Open the database .CSV and return the data as a list.
def getKnownEncodings(){}

# Compare the face with the given list of encodings and return the possible
# entry in the known encodings.
def compareFaces(){
	return 1
}