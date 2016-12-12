import time


class User():
    def __init__(self, name, avatar = "", role = 1):
        self.name = name
        self.avatar = avatar
        self.role = role


class Event():
    def __init__(self, name, category = None, latitude = 0, longtitude = 0, maxPeople = 10, description = "", tasks = None, date = int(time.time())):
        if tasks is None:
            tasks = []
        self.name = name
        self.category = category
        self.latitude = latitude
        self.longtitude = longtitude
        self.isJoined = False
        self.isReported = False
        self.maxPeople = maxPeople
        self.description = description
        self.isEnded = False
        self.tasks = tasks
        self.date = date

    def to_dict(self):
        dict = self.__dict__
        dict['category'] = self.category.__dict__
        tasks_dict = []
        for t  in self.tasks:
            tasks_dict.append(t.__dict__)
        dict['tasks'] = tasks_dict
        return dict


class Tasks():
    def __init__(self, message):
        self.message = message


class Category:
    def __init__(self, name):
        self.name = name



