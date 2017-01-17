import random
import string
import logging

def random_generator(size=32, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for x in range(size))


def random__string_number_generator(size=12, chars=string.digits):
    return ''.join(random.choice(chars) for x in range(size))


def random_latitude_near_moscow():
    return 55.55 + random.random() * 0.38


def random_longtitude_near_moscow():
    return 37 + random.random()

def random_distance():
    return 10 + 70*random.random()


def random_header_dict():
    return {"ClientId": random__string_number_generator(), "Token": random_generator()}

def random_url_get_events():
    return "/event/distance/{0}?latitude={1}&longtitude={2}".format(random_distance(),random_latitude_near_moscow(),random_longtitude_near_moscow())

# avg = 0.0
# min = 1000
# max = 0
# for i in range(1,1000):
#     rand = random_distance()
#     print(rand)
#     avg += rand
#     if rand > max:
#         max = rand
#     if rand < min:
#         min = rand
#
# print("AVERAGE: " + str(avg/1000))
# print("MIN: " + str(min))
# print("MAX: " + str(max))
#
# logging.basicConfig(filename="test2.txt", level=logging.INFO)
# for i in range(1,10):
#     s = random_url_get_events()
#     #print(s)
#     logging.info(s)
