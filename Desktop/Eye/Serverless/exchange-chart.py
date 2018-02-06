import datetime

try:
    import tkinter
    import urllib.request as urllib_request
    from urllib.error import URLError as urllib_URL_error
    from urllib.error import HTTPError as urllib_HTTP_error
except ImportError:
    import Tkinter as tkinter       # Pyton 2.7
    import urllib2 as urllib_request
    from urllib2 import URLError as urllib_URL_error
    from urllib2 import HTTPError as urllib_HTTP_error
import json


def draw_axes():
    canvas.configure(scrollregion=(0, -y_origin, x_origin / 2, y_origin + label_height))
    canvas.create_line(0, y_origin, x_origin, y_origin, width=2)
    canvas.create_line(0, 0, 0, y_origin * 2, width=2)


def print_label(x, label):
    canvas.create_text(x * bar_width * 2, y_origin, text=label, anchor='nw')


def draw_bar(x, y, bar_colour):
    x_pos = x * bar_width * 2 + bar_width
    y_height = y * bar_scaling
    canvas.create_rectangle(x_pos - bar_width + bar_spacing, y_origin - 2, x_pos + bar_width, y_origin - y_height,
                            fill=bar_colour, outline=bar_colour)


def generate_year_month(now):
    start_month = now.month - 11
    start_year = now.year
    if start_month < 1:
        start_month = 12 + start_month
        start_year -= 1

    for count in range(12):
        yield start_year, start_month
        start_month += 1
        if start_month > 12:
            start_year += 1
            start_month = 1


mainWindow = tkinter.Tk()

mainWindow.title("USD Exchange Rates")
mainWindow.geometry('1024x768')

canvas = tkinter.Canvas(mainWindow, width=800, height=600)
canvas.grid(row=1, column=0)
canvas.update()

x_origin = canvas.winfo_width()
y_origin = canvas.winfo_height() / 2

bar_width = 11
bar_spacing = 4
bar_scaling = 150
label_height = 40

draw_axes()

chart_data = [('AUD', 'blue'), ('GBP', 'red'), ('EUR', 'green')]

# draw the key
row_y_position = 0
for currency, colour in chart_data:
    canvas.create_text(x_origin / 2, row_y_position, text=currency, anchor='nw', fill=colour)
    row_y_position += 20

bar_x = 0

current_date = datetime.datetime.utcnow()
for year, month in generate_year_month(current_date):
    month_start = '{0}-{1:02d}-01'.format(year, month)
    try:
        data_values = urllib_request.urlopen('http://api.fixer.io/{}?base=USD'.format(month_start)).read()
    except urllib_HTTP_error:
        print("The exchange rates site is currently unavailable, please try again later.")
    except urllib_URL_error:
        print("Error in URL, please try again later.")
    else:
        dict1 = json.loads(data_values.decode('utf-8'))
        rates = dict1['rates']

        print_label(bar_x + 1, "{}\n{}".format(datetime.date(year, month, 1).strftime('%B')[:3], year))
        for currency, colour in chart_data:
            draw_bar(bar_x, rates[currency], colour)
            bar_x += 1

mainWindow.mainloop()
