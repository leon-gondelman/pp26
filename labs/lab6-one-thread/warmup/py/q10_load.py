# Questions 10 and 11. Run: python3 q10_load.py bare
#                     then: python3 q10_load.py task
import asyncio, sys

async def fetch_user():
    await asyncio.sleep(0.3)
    return "Ada"

async def load():
    print("A")
    user = await fetch_user()
    print("C", user)

async def bare():
    load()
    print("B")
    await asyncio.sleep(0.5)

async def task():
    asyncio.create_task(load())
    print("B")
    await asyncio.sleep(0.5)

asyncio.run({"bare": bare, "task": task}[sys.argv[1]]())
