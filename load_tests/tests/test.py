import json
import random

from locust import HttpLocust, TaskSet, task

from randoms.RandomEntities import random_user, random_event
from randoms.RandomUtils import random_header_dict, random_url_get_events


class MyTaskSet(TaskSet):
    def __init__(self, parent):
        super(MyTaskSet, self).__init__(parent)
        self.user = random_user()
        self.headers = random_header_dict()

    def on_start(self):
        self.client.post("/auth", json=self.user.__dict__,
                         headers=self.headers)

    @task(10)
    def index(self):
        self.client.get(headers=self.headers,
                        url=random_url_get_events())

    # @task(30)
    # def index(self):
    #     url = "/event/" + str(random.randint(1, 100))
    #     self.client.get(headers=self.headers,
    #                     url=url)

    @task(10)
    def index(self):
        url = "/event"
        self.client.get(headers=self.headers,
                        url=url)

    @task(4)
    def about(self):
        self.client.post("/event", json=random_event().to_dict(), headers=self.headers)


class MyLocust(HttpLocust):
    task_set = MyTaskSet
    min_wait = 1000
    max_wait = 2000
