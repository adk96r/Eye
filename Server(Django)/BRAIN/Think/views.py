from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.http import require_http_methods
from django.views.decorators.csrf import csrf_exempt
import json
import face_recognition
import base64

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
	possible_roll_number = 1210314802 + compare(known_encodings, unknown_encodings)

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
	try:
	encodings = list()
	with open('C:\\Users\\Pradeep Chandra\\Downloads\\encodings_128.csv', 'r') as c:
		reader = csv.reader(c, delimiter=';', dialect='excel')
		for r in reader:
			#rolls.append(int(r[0]))
			encodings.append([float(r[x]) for x in range(1, len(r))])
	return encodings
	except IndexError:
		print("IndexError Handled")

# Take a b64 file and convert it and save it as <imageFileName>.jpg.
def convertB64toImageAndSave(b64_string, image_file_name):

	#imgdata takes the decoded base64 string into imgdata
	#image_file_name is the file name where the image is stored
	#imgdata is returned
	imgdata = base64.b64decode(b64_string)
	image_file_name = 'some_image.jpg'
	with open('image_file_name','wb') as fo:
		fo.write(imgdata)
	return imgdata


# Open the given image file and extract the 128 encodings and return them.
def getEncodingsFromImageFile(image_file_name):

	#Just loading the image and encoding the Unknown image values
	unknown_image = face_recognition.load_image_file("image_file_name")
	unknown_encodings = face_recognition.face_encodings(unknown_image)[0]
	return unknown_encodings

# Compare the face with the given list of encodings and return the possible
# entry in the known encodings. Return -1 if nothing's been found.
def compare(known_encodings, unknown_encodings):
	results = fr.compare_faces([known_encodings], unknown_encodings, tolerance = tol)
	for m in range(0, len(results)):
		if results[m]:
			return m
	return -1
