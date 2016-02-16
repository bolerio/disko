Many implementations of dataflow processors process some input to produce some output in a rather stateless fashion. Such processors can be load balanced by creating copies of them and splitting the work between each copy. Suppose a processor P takes input from a single channel I and produces output to a single channel O:

```
I -> P -> O
```

so that for each incoming datum x, a single datum y is produced. If the transformation from x to y is time consuming and there are computing resources available, it would be beneficial to create two copies of P, say P1 and P2, and load balance the input between each of them while still making sure that the order of x's and y's is preserved:

```
I -> LoadBalance[P1, P2] -> O
```

where LoadBalance is a black box made up of the following operations:

```
I -> Split -> S1, S2

S1 -> P1 -> C1

S2 -> P2 -> C2

C1, C2 -> Combine -> O
```

The Split operation reads input from I and writes alternately to two new channels S1 and S2. S1 becomes the input channel of one of the P1 copy of P while S2 becomes the input channel of the P2 copy of P. Then P1 outputs to the new channel C1 while P2 outputs to the new channel C2. Finally, the Combine operation merge the data from channels C1 and C2 input O.

Thus to the outside world, the operation of processor P remains identical. It is replaced by a more complex black box, but the data travelling channels I and O remains exactly the same and as far as other parts of the system are concerned this is all that matters.

The situation becomes more complex when the original processor P has more than one input channel, for example two input channels:

```
I1, I2 -> P -> O
```

In this case, load balancing might still be possible, but it depends more on the internal logic of the processor. If the processor treats each input pair coming from I1 and I2 as a single argument set to whatever it is doing than splitting can be done along those lines: conceptually I1 and I2 are seen as a single channel I of tuples (x1:I1, x2:I2), and it is this channel of input tuples that is being split between the two copies P1 and P2. In such a situation, we can say that the channels I1 and I2 are synchronized.

A more complex scenario is when the processor reads more often from one of the input channels than from the other. This means that either there is some of sychronization between the two channels or there's none whatsoever. When there's no synchronization at all, this means that the inputs are completely independent and the splitter could split one or the other or both. When there's some synchronization, splitting could still be possible if the synchronization does not depend on the data itself, but follows some predetermined pattern like "for every 3 reads from I1, read once from I2).


Note that a similar difficulty does not arise in the case of multiple outputs. If we have:

```
I -> P -> O1, O2
```

Each of the outputs O1 and O2 are treated separately and independently.