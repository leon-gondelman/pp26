# Part C, elsewhere. The two deposits in Python. Required: 130.
import asyncio

async def nothing_to_wait_for(): pass
async def really_waits(): await asyncio.sleep(0)

async def trial(name, approve):
    account = {"balance": 100}
    async def deposit(n):
        b = account["balance"]
        await approve()
        account["balance"] = b + n
    await asyncio.gather(deposit(10), deposit(20))
    print(f"{name:42}", account["balance"])

asyncio.run(trial("approve() has nothing to wait for", nothing_to_wait_for))
asyncio.run(trial("approve() waits, for 0 seconds", really_waits))
