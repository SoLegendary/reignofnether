# The Process to Make a New Faction:

1. register the new faction via ```register()``` in ```Factions.java```
2. use ```.setXXX``` to set the property of the new faction (See the examples in ```Factions.java```)
3. set the entities' factions via ```registerEntity()``` and set the buildings' factions via ```registerBuilding()```
4. set the worker entity and the scout entity
5. set the start building (capital)

### **Notice that the process 3-5 needs to be done in the ```FMLCommonSetupEvent```**
### The sequence of registering is the sequence of the buttons shown in sandbox mode