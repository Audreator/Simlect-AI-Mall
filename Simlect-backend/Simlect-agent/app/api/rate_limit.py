# -*- coding: utf-8 -*-
"""全局限流器单例：避免 main.py 与 agent.py 各建一个 Limiter 导致限流计数分离、全局限流失效。"""
from slowapi import Limiter
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)
