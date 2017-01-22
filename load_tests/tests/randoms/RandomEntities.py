import RandomUtils
import random
import time
import datetime
from entities.Entities import Tasks, Event, Category, User


class RandomCategory():
    categories = []
    current = 0

    def __init__(self):
        for i in range(1, 25):
            self.categories.append(Category(RandomUtils.random_generator(6)))

    def create_category(self):
        idx = self.current
        self.current = (self.current + 1) % len(self.categories)
        return self.categories[idx]


random_category_creator = RandomCategory()


def random_task():
    return Tasks(RandomUtils.random_generator(6))


def random_user():
    return User(name=RandomUtils.random_generator(4))

def random_date():
    return int(round(time.time() * 1000)) + random.randint(5 * 1000 * 60 * 60 * 60,30 * 24 * 1000 * 60 * 60 * 60)

def random_event():
    tasks = []
    for i in range(0, random.randint(0, 2)):
        tasks.append(random_task())
    return Event(name=RandomUtils.random_generator(6),
                 category=random_category_creator.create_category(),
                 latitude=RandomUtils.random_latitude_near_moscow(),
                 longtitude=RandomUtils.random_longtitude_near_moscow(),
                 date= random_date(),
                 tasks=tasks)