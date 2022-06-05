# PrefectProj
Programming language made in Java as an experiment. Named 
"Prefect" with the hope that someone will mispronounce the name and call
 it the "Perfect" programming language(It's also the name of a car).


It has C-like syntax, static scoping, dynamic typing and 
it's Turing complete, as it should be. The only thing that is really 
missing is a proper standard library, and classes if OOP is your thing.


- Not made for production use, at least performance-wise. A version implemented in C would be far superior in that regard. Also, this version is interpreted, not compiled.

Example program written in Prefect:

```
// round, print, read are functions that are supposed to be part of the "standard library" :')

fun heapify(numbers, n, i)
{
    var largest = i;
    var l = 2 * i + 1;
    var r = 2 * i + 2;
    //print(numbers[largest]);
    if (l < n and numbers[l] > numbers[largest])
        largest = l;

    if (r < n and numbers[r] > numbers[largest])
        largest = r;

    if (largest != i)
    {
        var c = numbers[i];
        numbers[i] = numbers[largest];
        numbers[largest] = c;

        heapify(numbers, n, largest);
    }
}

fun heapSort(numbers, n)
{
    for (var i = round(n / 2) - 1; i >= 0; i = i - 1)
    {
        heapify(numbers, n, i);
    }

    for (var i = n - 1; i > 0; i = i - 1)
    {
        var c = numbers[i];
        numbers[i] = numbers[0];
        numbers[0] = c;

        heapify(numbers, i, 0);
    }
}

var n;
dictionary nr;
n = read();

for (var i = 0; i < n; i = i + 1)
{
    nr[i] = read();
}

heapSort(nr, n);

for (var i = 0; i < n; i = i + 1)
{
    print(nr[i]);
}
```
