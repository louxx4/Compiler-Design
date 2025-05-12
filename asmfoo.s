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
mov %r0, 5
mov %cl, 1
shl %r0
mov %r1, 5
mov %cl, 1
shl %r1
imul %r0, %r1
mov %cl, 2
shl %r0
mov %rax, %r0
ret
