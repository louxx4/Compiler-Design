.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq %rax, %rdi
# move the exit syscall number into rax
movq $0x3C, %rax
syscall
_main:
mov $69, %eax
mov $420, %r12d
cltq
cqto
idiv %r12d
mov %edx, %r12d
mov $420, %eax
cltq
cqto
idiv %r12d
mov %edx, %r13d
mov %r12d, %eax
cltq
cqto
idiv %r13d
mov %edx, %r14d
mov %r13d, %eax
cltq
cqto
idiv %r14d
mov %edx, %r12d
mov %r14d, %eax
cltq
ret
