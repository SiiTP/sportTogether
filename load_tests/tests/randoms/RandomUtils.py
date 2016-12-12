import random
import string


def random_generator(size=32, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for x in range(size))


def random__string_number_generator(size=12, chars=string.digits):
    return ''.join(random.choice(chars) for x in range(size))


def random_latitude_near_moscow():
    return 55.7 + random.random() * 0.1


def random_longtitude_near_moscow():
    return 37 + random.random()


def random_header_dict():
    return {"ClientId": random__string_number_generator(), "Token": random_generator()}
